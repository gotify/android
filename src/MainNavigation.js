import Home from './Home'
import {createStackNavigator} from 'react-navigation';
import Logs from "./Logs";

export default createStackNavigator({
    Home: {
        screen: Home,
    },
    Logs: {
        screen: Logs
    }
}, {initialRouteName: 'Home'});