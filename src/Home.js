import React from 'react';
import {Alert, Button, Image, ScrollView, Text, View} from "react-native";
import SharedPreferences from 'react-native-shared-preferences';

export default class Messages extends React.Component {
    static navigationOptions = ({navigation}) => {
        return {
            headerTitle: (
                <React.Fragment>
                    <Image source={require('../logo.png')} style={{width: 55, height: 55}}/>
                    <Text style={{fontSize: 25, marginLeft: 10}}>Gotify</Text>
                </React.Fragment>
            ),
            headerRight: (
                <View style={{paddingRight:10}}>
                    <Button
                        onPress={() => {
                            Alert.alert(
                                'Logout Confirmation',
                                'Do you really want to log out?',
                                [
                                    {
                                        text: 'Cancel', onPress: () => {
                                        }, style: 'cancel'
                                    },
                                    {
                                        text: 'OK', onPress: () => {
                                            SharedPreferences.removeItem("@global:token");
                                            navigation.navigate('AuthLoading');
                                        }
                                    },
                                ],
                                {cancelable: false}
                            )
                        }}
                        style={{backgroundColor: "#fff", color: "#000"}}
                        title="Logout"/>
                </View>
            ),
        };
    };

    render() {
        return (
            <ScrollView contentContainerStyle={{
                flex: 1,
                flexDirection: 'column',
                justifyContent: 'center',
                alignItems: 'center'
            }}>
                <Text style={{fontSize: 27, textAlign: 'center', marginBottom: 20}}>See notifications for status</Text>
                <Button onPress={() => this.props.navigation.navigate('Logs')} title="View Logs"/>
            </ScrollView>
        )
    }
}