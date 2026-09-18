import { Link, useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, StyleSheet, Text, View } from 'react-native';

import { AccountBar } from '@/components/account-bar';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import type { CartResponse } from '@/lib/types';

const money = (v: number) => `$${v.toFixed(2)}`;

export default function CartScreen() {
  const router = useRouter();
  const { token } = useAuth();

  const [cart, setCart] = useState<CartResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadCart = useCallback(async () => {
    try {
      setError(null);
      const c = await api<CartResponse>('/api/cart', { token });
      setCart(c);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load cart');
    } finally {
      setLoading(false);
    }
  }, [token]);

  useFocusEffect(
    useCallback(() => {
      loadCart();
    }, [loadCart]),
  );

  useEffect(() => {
    loadCart();
  }, [loadCart]);

  async function updateQuantity(cartItemId: number, quantity: number) {
    setBusy(cartItemId);
    try {
      await api(`/api/cart/items/${cartItemId}`, { method: 'PUT', body: { quantity }, token });
      await loadCart();
    } finally {
      setBusy(null);
    }
  }

  async function removeItem(cartItemId: number) {
    setBusy(cartItemId);
    try {
      await api(`/api/cart/items/${cartItemId}`, { method: 'DELETE', token });
      await loadCart();
    } finally {
      setBusy(null);
    }
  }

  async function clearCart() {
    setBusy(-1);
    try {
      await api('/api/cart', { method: 'DELETE', token });
      await loadCart();
    } finally {
      setBusy(null);
    }
  }

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  const items = cart?.items ?? [];

  return (
    <View style={styles.screen}>
      <AccountBar />
      {error ? (
        <View style={styles.center}>
          <Text style={styles.error}>{error}</Text>
        </View>
      ) : items.length === 0 ? (
        <View style={styles.center}>
          <Text style={styles.empty}>Your cart is empty</Text>
          <Link href="/catalog" style={styles.link}>
            <Text style={styles.linkText}>Browse products</Text>
          </Link>
        </View>
      ) : (
        <>
          <FlatList
            data={items}
            keyExtractor={(i) => String(i.cartItemId)}
            contentContainerStyle={styles.list}
            renderItem={({ item }) => (
              <View style={styles.card}>
                <View style={styles.cardTop}>
                  <Text style={styles.itemName}>{item.productName}</Text>
                  <Pressable onPress={() => removeItem(item.cartItemId)} disabled={busy === item.cartItemId}>
                    <Text style={styles.remove}>Remove</Text>
                  </Pressable>
                </View>
                <Text style={styles.itemMeta}>{money(item.unitPrice)} each</Text>
                <View style={styles.cardBottom}>
                  <View style={styles.stepper}>
                    <Pressable
                      onPress={() => updateQuantity(item.cartItemId, item.quantity - 1)}
                      disabled={busy === item.cartItemId || item.quantity <= 1}
                      style={styles.stepBtn}
                    >
                      <Text style={styles.stepText}>−</Text>
                    </Pressable>
                    <Text style={styles.qtyText}>{item.quantity}</Text>
                    <Pressable
                      onPress={() => updateQuantity(item.cartItemId, item.quantity + 1)}
                      disabled={busy === item.cartItemId}
                      style={styles.stepBtn}
                    >
                      <Text style={styles.stepText}>+</Text>
                    </Pressable>
                  </View>
                  <Text style={styles.subtotal}>{money(item.subtotal)}</Text>
                </View>
              </View>
            )}
            ListFooterComponent={
              <View style={styles.footer}>
                <Pressable onPress={clearCart} disabled={busy === -1}>
                  <Text style={styles.clear}>Clear cart</Text>
                </Pressable>
              </View>
            }
          />
          <View style={styles.summary}>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>Total</Text>
              <Text style={styles.summaryValue}>{money(cart?.totalPrice ?? 0)}</Text>
            </View>
            <Pressable style={styles.checkoutBtn} onPress={() => router.push('/checkout')}>
              <Text style={styles.checkoutText}>Proceed to checkout</Text>
            </Pressable>
          </View>
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#f3f4f6' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12, padding: 24 },
  error: { color: '#b91c1c', fontSize: 15 },
  empty: { color: '#6b7280', fontSize: 15 },
  link: { padding: 8 },
  linkText: { color: '#2563eb', fontSize: 15, fontWeight: '600' },
  list: { padding: 16, gap: 12 },
  card: { backgroundColor: '#ffffff', borderRadius: 12, padding: 14, gap: 8 },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  itemName: { fontSize: 15, fontWeight: '700', color: '#111827', flex: 1 },
  remove: { color: '#b91c1c', fontSize: 13, fontWeight: '600', paddingLeft: 8 },
  itemMeta: { fontSize: 13, color: '#6b7280' },
  cardBottom: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  stepper: { flexDirection: 'row', alignItems: 'center', borderWidth: 1, borderColor: '#d1d5db', borderRadius: 8 },
  stepBtn: { paddingHorizontal: 12, paddingVertical: 6 },
  stepText: { fontSize: 16, fontWeight: '700', color: '#374151' },
  qtyText: { fontSize: 15, fontWeight: '700', paddingHorizontal: 8, minWidth: 28, textAlign: 'center' },
  subtotal: { fontSize: 16, fontWeight: '800', color: '#111827' },
  footer: { alignItems: 'center', paddingVertical: 8 },
  clear: { color: '#b91c1c', fontSize: 14, fontWeight: '600' },
  summary: {
    backgroundColor: '#ffffff',
    padding: 16,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: '#d1d5db',
    gap: 10,
  },
  summaryRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  summaryLabel: { fontSize: 16, color: '#374151' },
  summaryValue: { fontSize: 22, fontWeight: '800', color: '#111827' },
  checkoutBtn: { backgroundColor: '#2563eb', borderRadius: 8, paddingVertical: 14, alignItems: 'center' },
  checkoutText: { color: '#ffffff', fontSize: 16, fontWeight: '700' },
});