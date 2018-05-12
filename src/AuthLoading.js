import React from 'react';
import {
    ActivityIndicator,
    AsyncStorage,
    StatusBar,
    StyleSheet,
    View,
} from 'react-native';
import SharedPreferences from 'react-native-shared-preferences';

export default class AuthLoadingScreen extends React.Component {
    componentDidMount() {
        SharedPreferences.getItem('@global:token', (token) => {
            this.props.navigation.navigate(token ? 'Messages' : 'Login');
        });
    }

    render() {
        return (
            <View >
                <ActivityIndicator />
                <StatusBar barStyle="default" />
            </View>
        );
    }
}