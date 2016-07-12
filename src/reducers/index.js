import contacts from './contacts';
import events from './events';
import ui from './ui';
import rotations from './rotations';

export function reducer(state = {}, action) {
  return {
    contacts: contacts(state.contacts, action),
    events: events(state.events, action),
    ui: ui(state.ui, action),
    rotations: rotations(state.rotations, action)
  };
}
