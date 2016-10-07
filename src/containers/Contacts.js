import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { View, Text, Navigator } from 'react-native';
import LinearGradient from 'react-native-linear-gradient';
import ContactList from '../components/ContactList';
import AddContactButton from '../components/AddContactButton';
import AddContactView from '../components/AddContactView/AddContactView';
import { CONTACT_TYPE } from '../data/constants';

import * as Actions from '../actions';
import SingleContactView from '../components/SingleContactView/SingleContactView';

function filterPrimaryContactsOnly(contacts) {
  return contacts.filter((contact) => contact.connection === CONTACT_TYPE.PRIMARY);
}

function mapStateToProps(state) {
  return {
    contacts: state.contacts,
    initialStoreLoaded: state.ui.initialStoreLoaded
  };
}

function mapDispatchToActions(dispatch) {
  return {
    actions: bindActionCreators(Actions, dispatch)
  };
}

const ContactsComponent = (props) => {
  const routes = [
    { title: 'Contacts', index: 0 }
  ];
  return (
    <LinearGradient colors={['#F7F7F7', '#D7D7D7']} style={styles.container}>
      <Navigator
        initialRoute={routes[0]}
        initialRouteStack={routes}
        renderScene={(route, navigator) => {
          if (route.index === 0) {
            return (
              <View>
                <AddContactButton onPress={() => navigator.push({ title: 'Add Contact', index: 2 })} />
                <ContactList
                  contacts={filterPrimaryContactsOnly(props.contacts)}
                  onNavigatePress={(title, contactId) =>
                    navigator.push({ title, contactId, index: 1 })}
                />
              </View>
            );
          } else if (route.index === 1) {
            return (
              <SingleContactView
                contactId={route.contactId}
                onNavigatePress={() => navigator.pop()}
              />);
          } else if (route.index === 2) {
            return <AddContactView onBack={() => navigator.pop()} />;
          }
          return <View><Text>oops unexpected route</Text></View>;
        }}
        configureScene={() =>
          Navigator.SceneConfigs.PushFromRight}
      />
    </LinearGradient>
  );
};

ContactsComponent.propTypes = {
  contacts: PropTypes.array.isRequired,
  actions: PropTypes.objectOf(PropTypes.func),
  initialStoreLoaded: PropTypes.bool,
  modalVisible: PropTypes.bool,
};

const styles = {
  container: {
    flex: 1,
  }
};

export default connect(mapStateToProps, mapDispatchToActions)(ContactsComponent);
