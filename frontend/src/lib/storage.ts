import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const TOKEN_KEY = 'tradehub.jwt';

async function getTokenNative(): Promise<string | null> {
  return SecureStore.getItemAsync(TOKEN_KEY);
}

async function setTokenNative(token: string): Promise<void> {
  if (token) {
    await SecureStore.setItemAsync(TOKEN_KEY, token);
  } else {
    await SecureStore.deleteItemAsync(TOKEN_KEY);
  }
}

// Web fallback: SecureStore is unavailable in browsers.
function getTokenWeb(): string | null {
  return window.localStorage.getItem(TOKEN_KEY);
}

function setTokenWeb(token: string): void {
  if (token) {
    window.localStorage.setItem(TOKEN_KEY, token);
  } else {
    window.localStorage.removeItem(TOKEN_KEY);
  }
}

export async function getToken(): Promise<string | null> {
  if (Platform.OS === 'web') {
    return getTokenWeb();
  }
  return getTokenNative();
}

export async function setToken(token: string): Promise<void> {
  if (Platform.OS === 'web') {
    return setTokenWeb(token);
  }
  return setTokenNative(token);
}