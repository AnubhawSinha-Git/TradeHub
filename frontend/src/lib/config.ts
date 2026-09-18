import Constants from 'expo-constants';
import { Platform } from 'react-native';

function resolveBaseUrl(): string {
  // On web the browser and the API are on the same machine.
  if (Platform.OS === 'web') {
    return 'http://localhost:8080';
  }

  // Native only: Expo Go exposes the dev machine's address as hostUri
  // (e.g. "192.168.1.10:8081"). Reuse that host with the API port.
  const hostUri = Constants.expoConfig?.hostUri;
  const host = hostUri?.split(':')[0];

  if (host && host !== 'localhost' && host !== '127.0.0.1') {
    return `http://${host}:8080`;
  }

  // Fallbacks: Android emulator loopback, then localhost.
  if (Platform.OS === 'android') {
    return 'http://10.0.2.2:8080';
  }

  return 'http://localhost:8080';
}

export const API_BASE = resolveBaseUrl();