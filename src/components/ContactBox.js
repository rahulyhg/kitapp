
import React, { PropTypes } from 'react';
import { View, Text, TouchableOpacity } from 'react-native';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import LinearGradient from 'react-native-linear-gradient';
import Icon from 'react-native-vector-icons/FontAwesome';

import * as Actions from '../actions';

const editIcon = (<Icon name="edit" size={30} color="white" />);

function mapStateToProps(state) {
  return {
    modalVisible: state.ui.contactModalVisible
  };
}

function mapDispatchToActions(dispatch) {
  return {
    actions: bindActionCreators(Actions, dispatch)
  };
}

const ContactBox = (props) => {
  const colors = colorMap.meet;
  return (
    <TouchableOpacity
      onPress={() => {
        props.actions.setSelectedContact(props.contact.id);
        props.actions.setModalVisibility(true);
      }}
    >
      <LinearGradient colors={colors} style={styles.container}>
        <View style={styles.contactName}>
          <Text style={styles.contactNameText} numberOfLines={1}>
              {props.contact.name}
          </Text>
        </View>
        <View style={styles.icon}>
            {editIcon}
        </View>
      </LinearGradient>
    </TouchableOpacity>
  );
};

ContactBox.propTypes = {
  contact: PropTypes.object,
  actions: PropTypes.object,
  modalVisible: PropTypes.bool,
};

const colorMap = {
  meet: ['#FF5E3A', '#FF2A68'],
  call: ['#74DF5F', '#09B014']
};

const styles = {
  container: {
    height: 50,
    flexDirection: 'row'
  },
  date: {
    width: 50,
    alignItems: 'center'
  },
  month: {
    marginTop: 3
  },
  day: {
    borderWidth: 1,
    borderColor: 'white',
    height: 26,
    width: 26,
    borderRadius: 13,
    alignItems: 'center',
    justifyContent: 'center'
  },
  monthText: {
    color: 'white',
    fontSize: 10,
    fontWeight: 'bold'
  },
  dayText: {
    color: 'white'
  },
  contactName: {
    flex: 1,
    marginLeft: 10,
    justifyContent: 'center'
  },
  contactNameText: {
    color: 'white'
  },
  icon: {
    width: 50,
    alignItems: 'center',
    justifyContent: 'center'
  }
};

export default connect(mapStateToProps, mapDispatchToActions)(ContactBox);
