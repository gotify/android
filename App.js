import AuthLoading from './src/AuthLoading'
import Messages from './src/Messages'
import Login from './src/Login'
import { createSwitchNavigator } from 'react-navigation';

export default createSwitchNavigator({
    Messages: {
        screen: Messages,
    },
    Login: {
        screen: Login,
    },
    AuthLoading: {
        screen: AuthLoading
    },
}, {initialRouteName: 'AuthLoading'});