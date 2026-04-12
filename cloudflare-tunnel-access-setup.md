# Self-Hosted App with Cloudflare Tunnel & Access Protection

This guide covers how to expose a self-hosted application through a Cloudflare Tunnel and protect it with Cloudflare Access, including service token authentication for API clients.

---

## Prerequisites

- A domain managed by Cloudflare (e.g. `example.com`)
- `cloudflared` installed and running on your server
- An existing Cloudflare Tunnel configured
- A self-hosted app running on your local network (e.g. `http://192.168.0.105:8080`)

---

## 1. Add the Hostname to Your Tunnel

Edit your cloudflared configuration file:

```bash
nano /etc/cloudflared/config.yml
```

Add a new hostname entry **before** the catch-all rule:

```yaml
tunnel: <your-tunnel-id>
credentials-file: /root/.cloudflared/<your-tunnel-id>.json
ingress:
  - hostname: myapp.example.com
    service: http://localhost:3000
  - hostname: newapp.example.com
    service: http://<INTERNAL_IP>:<PORT>
  - service: http_status:404
```

> **Important:** The catch-all rule (`- service: http_status:404`) must always be the last entry.

Restart cloudflared to apply changes:

```bash
systemctl restart cloudflared
```

---

## 2. Verify the DNS Record

When you add a hostname to the tunnel, Cloudflare usually creates the DNS record automatically. Verify it exists:

1. Go to [Cloudflare Dashboard](https://dash.cloudflare.com/)
2. Select your domain → **DNS** → **Records**
3. Confirm a **CNAME** record exists for your subdomain pointing to `<tunnel-id>.cfargotunnel.com`

If it's missing, create it manually:

| Field        | Value                              |
|--------------|-------------------------------------|
| Type         | CNAME                              |
| Name         | `newapp`                           |
| Target       | `<tunnel-id>.cfargotunnel.com`     |
| Proxy status | Proxied (orange cloud on)          |

> **Tip:** Check your existing DNS records for other subdomains on the same tunnel — they'll have the same target value.

---

## 3. Protect the App with Cloudflare Access

### 3.1 Create an Access Application

1. Go to [Cloudflare Zero Trust](https://one.dash.cloudflare.com/)
2. Navigate to **Access** → **Applications**
3. Click **Add an application** → **Self-hosted**
4. Configure:
   - **Application name:** e.g. "My App"
   - **Session duration:** How long browser users stay authenticated before re-login (e.g. 24 hours, 7 days)
   - **Application domain:** `newapp.example.com`
5. Continue to policies

> **Note:** Session duration only applies to browser-based users. Service tokens authenticate on every request independently.

### 3.2 Create a Service Token

Before adding policies, create a service token for API/programmatic access:

1. Go to **Access** → **Service Auth** → **Service Tokens**
2. Click **Create Service Token**
3. Give it a name (e.g. "My App API Token")
4. **Save both values immediately:**
   - `CF-Access-Client-Id` (ends in `.access`)
   - `CF-Access-Client-Secret`

> **Warning:** The secret is only shown once at creation time. If you lose it, you'll need to create a new token.

### 3.3 Add the Service Auth Policy

This policy allows programmatic access via service tokens:

1. In your Access application, go to the **Policies** tab
2. Click **Add a policy**
3. Configure:
   - **Policy name:** e.g. "API Service Token"
   - **Action:** **Service Auth** (not Allow)
   - **Include** → **Service Token** → select your token
4. Save

> **Critical:** The action must be **Service Auth**, not **Allow**. If set to Allow, Cloudflare will recognize the token but still redirect to the login page.

### 3.4 Add a Browser Access Policy (Optional)

If you also want to access the app from a browser:

1. **Add a policy**
2. Configure:
   - **Policy name:** e.g. "Browser Access"
   - **Action:** **Allow**
   - **Include** → **Emails** → your email address (or **IP Ranges** for your public IP)
3. Save

With an email selector, Cloudflare will send you a one-time code to verify your identity. With an IP range selector, access is granted automatically from your network.

---

## 4. Using the Service Token in API Requests

Add these two headers to every API request:

```
CF-Access-Client-Id: <your-client-id>.access
CF-Access-Client-Secret: <your-client-secret>
```

### Example with curl

```bash
curl -H "CF-Access-Client-Id: YOUR_CLIENT_ID" \
     -H "CF-Access-Client-Secret: YOUR_CLIENT_SECRET" \
     https://newapp.example.com/api/endpoint
```

### Example with Python (requests)

```python
import requests

headers = {
    "CF-Access-Client-Id": "YOUR_CLIENT_ID",
    "CF-Access-Client-Secret": "YOUR_CLIENT_SECRET",
}

response = requests.get("https://newapp.example.com/api/endpoint", headers=headers)
```

---

## 5. Verify the Setup

Test that the service token works:

```bash
curl -v \
  -H "CF-Access-Client-Id: YOUR_CLIENT_ID" \
  -H "CF-Access-Client-Secret: YOUR_CLIENT_SECRET" \
  https://newapp.example.com/
```

**Expected result:** HTTP `200` response with your app's content.

**If you get a `302` redirect to the login page**, check the following:

- The policy action is **Service Auth**, not **Allow**
- The `CF-Access-Client-Id` and `CF-Access-Client-Secret` header names are exactly correct (case-sensitive)
- The token values match what was generated (the ID ends in `.access`)
- The service token is active and not expired (check in **Access** → **Service Auth**)

If the token is recognized but still redirects, delete the policy and recreate it with the correct **Service Auth** action.

---

## Summary

| Step | Where | What |
|------|-------|------|
| 1 | Server | Add hostname to `/etc/cloudflared/config.yml` and restart cloudflared |
| 2 | Cloudflare DNS | Verify CNAME record points to the tunnel |
| 3 | Zero Trust → Access | Create application, service token, and policies |
| 4 | Client app | Add `CF-Access-Client-Id` and `CF-Access-Client-Secret` headers |
| 5 | Terminal | Test with `curl -v` to confirm `200` response |
