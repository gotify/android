import AuthLoading from './src/AuthLoading'
import Login from './src/Login'
import { createSwitchNavigator } from 'react-navigation';
import MainNavigation from "./src/MainNavigation";

export default createSwitchNavigator({
    MainNavigation: {
        screen: MainNavigation,
    },
    Login: {
        screen: Login,
    },
    AuthLoading: {
        screen: AuthLoading
    },
}, {initialRouteName: 'AuthLoading'});