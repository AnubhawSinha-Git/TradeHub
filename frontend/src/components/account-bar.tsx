import { Link, useRouter } from 'expo-router';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAuth } from '@/lib/auth-context';

export function AccountBar() {
  const router = useRouter();
  const { user, logout } = useAuth();

  return (
    <View style={styles.bar}>
      <View style={styles.userInfo}>
        <Text style={styles.name} numberOfLines={1}>
          {user?.fullName ?? 'Guest'}
        </Text>
        <Text style={styles.email} numberOfLines={1}>
          {user?.email ?? ''}
        </Text>
      </View>

      <Link href="/cart" style={styles.action}>
        <Text style={styles.actionText}>Cart</Text>
      </Link>
      <Link href="/orders" style={styles.action}>
        <Text style={styles.actionText}>Orders</Text>
      </Link>
      <Pressable
        style={styles.action}
        onPress={async () => {
          await logout();
          router.replace('/login');
        }}
      >
        <Text style={styles.actionText}>Sign out</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: '#ffffff',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#d1d5db',
  },
  userInfo: { flex: 1 },
  name: { fontSize: 14, fontWeight: '700', color: '#111827' },
  email: { fontSize: 12, color: '#6b7280' },
  action: { paddingVertical: 6, paddingHorizontal: 8 },
  actionText: { color: '#2563eb', fontSize: 14, fontWeight: '600' },
});