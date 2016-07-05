/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, { Component } from 'react';
import {
  AppRegistry
} from 'react-native';
import KitApp from './src/KitApp.js';

const Main = () => (
  <KitApp />
);

AppRegistry.registerComponent('kitapp', () => Main);
