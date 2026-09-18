import { useFocusEffect } from 'expo-router';
import React, { useCallback, useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, StyleSheet, Text, View } from 'react-native';

import { AccountBar } from '@/components/account-bar';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import type { OrderResponse } from '@/lib/types';

const money = (v: number) => `$${v.toFixed(2)}`;

const STATUS_COLOR: Record<string, string> = {
  PLACED: '#b45309',
  PAID: '#047857',
  SHIPPED: '#1d4ed8',
  COMPLETED: '#065f46',
  CANCELLED: '#b91c1c',
  REFUNDED: '#6b7280',
};

export default function OrdersScreen() {
  const { token } = useAuth();

  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadOrders = useCallback(async () => {
    try {
      setError(null);
      const all = await api<OrderResponse[]>('/api/orders', { token });
      setOrders(all);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load orders');
    } finally {
      setLoading(false);
    }
  }, [token]);

  useFocusEffect(
    useCallback(() => {
      loadOrders();
    }, [loadOrders]),
  );

  async function pay(orderId: number) {
    setPaying(orderId);
    try {
      await api<OrderResponse>(`/api/orders/${orderId}/pay`, { method: 'POST', token });
      await loadOrders();
    } finally {
      setPaying(null);
    }
  }

  return (
    <View style={styles.screen}>
      <AccountBar />
      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" />
        </View>
      ) : error ? (
        <View style={styles.center}>
          <Text style={styles.error}>{error}</Text>
        </View>
      ) : orders.length === 0 ? (
        <View style={styles.center}>
          <Text style={styles.empty}>No orders yet</Text>
        </View>
      ) : (
        <FlatList
          data={orders}
          keyExtractor={(o) => String(o.orderId)}
          contentContainerStyle={styles.list}
          renderItem={({ item }) => (
            <View style={styles.card}>
              <View style={styles.cardTop}>
                <Text style={styles.orderId}>Order #{item.orderId}</Text>
                <Text style={[styles.status, { color: STATUS_COLOR[item.status] ?? '#374151' }]}>
                  {item.status}
                </Text>
              </View>
              <Text style={styles.date}>{new Date(item.createdAt).toLocaleString()}</Text>

              {item.items.map((line) => (
                <View key={line.orderItemId} style={styles.line}>
                  <Text style={styles.lineName}>
                    {line.productName} × {line.quantity}
                  </Text>
                  <Text style={styles.linePrice}>{money(line.subtotal)}</Text>
                </View>
              ))}

              {item.discountAmount > 0 && item.couponCode ? (
                <View style={styles.line}>
                  <Text style={styles.lineName}>Coupon {item.couponCode}</Text>
                  <Text style={styles.discount}>−{money(item.discountAmount)}</Text>
                </View>
              ) : null}

              <View style={styles.divider} />
              <View style={styles.line}>
                <Text style={styles.totalLabel}>Total</Text>
                <Text style={styles.totalValue}>{money(item.totalAmount)}</Text>
              </View>

              {item.status === 'PLACED' ? (
                <Pressable
                  style={[styles.payBtn, paying === item.orderId && styles.disabled]}
                  onPress={() => pay(item.orderId)}
                  disabled={paying === item.orderId}
                >
                  <Text style={styles.payBtnText}>{paying === item.orderId ? 'Paying…' : 'Pay now'}</Text>
                </Pressable>
              ) : null}
            </View>
          )}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#f3f4f6' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  error: { color: '#b91c1c', fontSize: 15 },
  empty: { color: '#6b7280', fontSize: 15 },
  list: { padding: 16, gap: 12 },
  card: { backgroundColor: '#ffffff', borderRadius: 12, padding: 14, gap: 6 },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  orderId: { fontSize: 16, fontWeight: '800', color: '#111827' },
  status: { fontSize: 13, fontWeight: '700' },
  date: { fontSize: 12, color: '#6b7280' },
  line: { flexDirection: 'row', justifyContent: 'space-between', gap: 12 },
  lineName: { flex: 1, color: '#374151', fontSize: 14 },
  linePrice: { color: '#111827', fontSize: 14, fontWeight: '600' },
  discount: { color: '#047857', fontSize: 14, fontWeight: '600' },
  divider: { height: StyleSheet.hairlineWidth, backgroundColor: '#d1d5db' },
  totalLabel: { fontSize: 15, fontWeight: '700', color: '#374151' },
  totalValue: { fontSize: 17, fontWeight: '800', color: '#111827' },
  payBtn: { backgroundColor: '#2563eb', borderRadius: 8, paddingVertical: 12, alignItems: 'center', marginTop: 6 },
  payBtnText: { color: '#ffffff', fontSize: 15, fontWeight: '700' },
  disabled: { opacity: 0.6 },
});