import React from "react";
import {ScrollView, Text, ToastAndroid} from "react-native";
import LogManager from "./native/LogManager";
import Icon from 'react-native-vector-icons/Ionicons'

export default class Logs extends React.Component {
    static navigationOptions = {
        headerTitle: <Text style={{fontSize: 25, marginLeft: 10}}>Logs</Text>,
        headerRight: (
            <Icon.Button
                name="md-trash"
                onPress={() => {
                    ToastAndroid.show('Clearing logs...', ToastAndroid.SHORT);
                    LogManager.clear(() => ToastAndroid.show('Cleared logs.', ToastAndroid.SHORT));
                }}
                color="#000"
                size={30}
                backgroundColor="#fff"/>
        ),
    };

    constructor() {
        super();
        this.id = 0;
        this.state = {logs: ''}
    }

    refresh = () => {
        LogManager.getLog((data) => {
            this.setState({logs: data})
        });
    };

    componentDidMount() {
        this.refresh();
        this.id = window.setInterval(this.refresh, 1000);
    }

    componentWillUnmount() {
        clearInterval(this.id);
    }

    render() {
        const {logs} = this.state;
        return (
            <ScrollView contentContainerStyle={{padding: 5}}>
                <Text style={{fontFamily: 'monospace', fontSize: 10}}>{logs}</Text>
            </ScrollView>
        )
    }
}