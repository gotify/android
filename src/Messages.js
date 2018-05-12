import React from 'react';
import {Button, Text, View} from "react-native";
import SharedPreferences from 'react-native-shared-preferences';

export default class Messages extends React.Component {
    render() {
        SharedPreferences.getAll(console.log);
        return (
            <View>
                <Text>TODO</Text>
                <Button onPress={() => {
                    SharedPreferences.removeItem("@global:token");
                    this.props.navigation.navigate('AuthLoading');
                }} title="Logout"/>
            </View>
        )
    }
}