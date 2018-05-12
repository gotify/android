import React from 'react';
import { View, Text, TextInput, Button, ToastAndroid } from 'react-native';
import SharedPreferences from 'react-native-shared-preferences';
import Icon from 'react-native-vector-icons/MaterialIcons'
import DeviceInfo from 'react-native-device-info';
import * as axios from "axios";

const urlRegex = new RegExp("^(http|https)://", "i");
const defaultClientName = DeviceInfo.getManufacturer() + ' ' + DeviceInfo.getDeviceId();
export default class Login extends React.Component {
    state = {tryConnect: null, url: null, error: null, version: null, name: '', pass: '', loggedIn: null, client: defaultClientName};
    componentDidMount() {
        SharedPreferences.getItem("@global:url", (url) => {
            if (url) {
                this.setState({...this.state, url: url}, this.checkUrl);
            }
        });
    }

    handleChange = (name, merge) => {
        return (val) => {
            const old = {...this.state, ...(merge || {})};
            old[name] = val;
            this.setState(old);
        }
    };

    reachedButNot = () => {
        this.setState({...this.state, tryConnect: false, error: 'server exists, but it is not a valid gotify instance'})
    };

    notReachable = () => {
        this.setState({...this.state, tryConnect: false, error: 'could not reach ' + this.state.url})
    };

    createClient = () => {
        const {client, url, pass, name}  = this.state;
        axios.post(url + 'client', {name: client}, {auth:{username: name, password: pass}}).then((resp) => {
            SharedPreferences.setItem('@global:token', resp.data.token);
            ToastAndroid.show("Created client " + client + " for user " + name, ToastAndroid.SHORT);
            this.props.navigation.navigate('AuthLoading')
        }).catch(() => {
            ToastAndroid.show("Could not create client", ToastAndroid.SHORT);
        })
    };

    version(url, errorCallback, reachedButNoApi) {
        axios.get(url + 'version').then((resp) => {
            if (resp && resp.status === 200) {
                const {data} = resp;
                if (data
                    && typeof data === 'object'
                    && 'version' in data
                    && 'buildDate' in data
                    && 'commit' in data
                ) {
                    SharedPreferences.setItem('@global:url', url);
                    this.setState({...this.state, url: url, tryConnect: true, error: null, version: data});
                    return;
                }
            }
            (reachedButNoApi ? reachedButNoApi : errorCallback)();
        }).catch(errorCallback);
    }

    checkUrl = () => {
        let {url} = this.state;
        if (!url.endsWith('/')) {
            url += '/';
        }

        if (urlRegex.test(url)) {
            if(url.match(/^http:\/\//i)){
                this.version(url.replace(/^http:\/\//i,"https://"), () => {
                    this.version(url, this.notReachable, this.reachedButNot)
                });
            } else {
                this.version(url, this.notReachable,this.reachedButNot)
            }
        } else {
            this.setState({...this.state, error: 'url must either start with http:// or https://'})
        }
    };

    checkUser = () => {
        const {url, pass, name}  = this.state;
        axios.get(url + 'current/user', {auth:{username: name, password: pass}}).then((resp) => {
            this.setState({...this.state, loggedIn: true})
        }).catch((resp) => {
            this.setState({...this.state, loggedIn: false})
        })
    };

    render() {
        const {tryConnect, error, version, url, client, loggedIn} = this.state;

        return (
            <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center'}}>
                <View style={{width: "80%"}}>
                    <Text style={{fontSize: 20, textAlign:'center'}}>Gotify</Text>
                    <View style={{ alignItems: 'center', flexDirection: 'row'}}>
                        <TextInput
                            value={url}
                            placeholder="URL f.ex https://push.domain.tld"
                            style={{alignSelf: 'stretch', flex: 1}}
                            keyboardType="url"
                            onChangeText={this.handleChange('url', {tryConnect: false, version: null})}/>
                        <Icon.Button name="send" style={{alignSelf: 'flex-end'}} iconStyle={{marginRight: 0}} onPress={this.checkUrl}/>
                    </View>
                    {error && <Text style={{textAlign:'center', color: 'red'}}>{error}</Text>}
                    {version && <Text style={{textAlign:'center', color: 'green'}}>Gotify v{version.version}</Text>}
                    {tryConnect === true && (
                        <React.Fragment>
                            <TextInput placeholder="Username"
                                       onChangeText={this.handleChange('name', {loggedIn: null})}
                                       style={{width: '100%'}}/>
                            <View style={{ alignItems: 'center', flexDirection: 'row'}}>

                            <TextInput placeholder="Password"
                                       secureTextEntry={true}
                                       onChangeText={this.handleChange('pass', {loggedIn: null})}
                                       style={{alignSelf: 'stretch', flex: 1}}
                            />
                                <Icon.Button name="send" style={{alignSelf: 'flex-end'}} iconStyle={{marginRight: 0}} onPress={this.checkUser}/>
                            </View>
                            {loggedIn === true && (
                                <React.Fragment>

                                    <Text  style={{textAlign:'center', color: 'green'}}>
                                        valid user
                                    </Text>

                                    <TextInput placeholder="Client Name"
                                               value={client}
                                               onChangeText={this.handleChange('client')}
                                               style={{width: '100%'}}/>
                                    <Button title="Create Client" onPress={this.createClient} />

                                </React.Fragment>
                            )}
                            {loggedIn === false && (
                                <Text  style={{textAlign:'center', color: 'red'}}>
                                    wrong name / pass
                                </Text>
                            )}

                        </React.Fragment>
                    )}
                </View>
            </View>

        );
    }
}