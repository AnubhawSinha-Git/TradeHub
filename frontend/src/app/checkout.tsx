import { useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import type { CartResponse, OrderResponse } from '@/lib/types';

const money = (v: number) => `$${v.toFixed(2)}`;

const emptyShipment = {
  recipientName: '',
  addressLine: '',
  addressLine2: '',
  city: '',
  state: '',
  zipCode: '',
  country: '',
  phone: '',
};

export default function CheckoutScreen() {
  const router = useRouter();
  const { token } = useAuth();

  const [cart, setCart] = useState<CartResponse | null>(null);
  const [shipment, setShipment] = useState(emptyShipment);
  const [couponCode, setCouponCode] = useState('');
  const [loadingCart, setLoadingCart] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [placed, setPlaced] = useState<OrderResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<CartResponse>('/api/cart', { token })
      .then(setCart)
      .finally(() => setLoadingCart(false));
  }, [token]);

  function setField(key: keyof typeof shipment, value: string) {
    setShipment((s) => ({ ...s, [key]: value }));
  }

  async function placeOrder() {
    if (submitting) return;
    setError(null);
    setSubmitting(true);
    try {
      const order = await api<OrderResponse>('/api/orders/checkout', {
        method: 'POST',
        body: { shipment, couponCode: couponCode.trim() || null },
        token,
      });
      const paid = await api<OrderResponse>(`/api/orders/${order.orderId}/pay`, {
        method: 'POST',
        token,
      });
      setPlaced(paid);
      setCart(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Checkout failed');
    } finally {
      setSubmitting(false);
    }
  }

  if (placed) {
    return (
      <SafeAreaView style={styles.screen}>
        <View style={styles.successBody}>
          <Text style={styles.successTitle}>Order placed & paid</Text>
          <Text style={styles.successSub}>Order #{placed.orderId}</Text>
          <Text style={styles.successTotal}>{money(placed.totalAmount)}</Text>
          {placed.discountAmount > 0 && placed.couponCode ? (
            <Text style={styles.successDiscount}>
              Coupon {placed.couponCode} saved {money(placed.discountAmount)}
            </Text>
          ) : null}
          <Pressable style={styles.primaryBtn} onPress={() => router.replace('/orders')}>
            <Text style={styles.primaryBtnText}>View my orders</Text>
          </Pressable>
          <Pressable onPress={() => router.replace('/catalog')}>
            <Text style={styles.secondaryLink}>Continue shopping</Text>
          </Pressable>
        </View>
      </SafeAreaView>
    );
  }

  const total = cart?.totalPrice ?? 0;

  return (
    <SafeAreaView style={styles.screen} edges={['bottom']}>
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        {loadingCart ? (
          <View style={styles.center}>
            <ActivityIndicator size="large" />
          </View>
        ) : !cart || cart.items.length === 0 ? (
          <View style={styles.center}>
            <Text style={styles.empty}>Your cart is empty — nothing to check out.</Text>
          </View>
        ) : (
          <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
            <Text style={styles.sectionTitle}>Shipping address</Text>
            <TextInput
              style={styles.input}
              placeholder="Recipient name *"
              value={shipment.recipientName}
              onChangeText={(v) => setField('recipientName', v)}
            />
            <TextInput
              style={styles.input}
              placeholder="Address line *"
              value={shipment.addressLine}
              onChangeText={(v) => setField('addressLine', v)}
            />
            <TextInput
              style={styles.input}
              placeholder="Address line 2"
              value={shipment.addressLine2}
              onChangeText={(v) => setField('addressLine2', v)}
            />
            <View style={styles.row}>
              <TextInput
                style={[styles.input, styles.half]}
                placeholder="City *"
                value={shipment.city}
                onChangeText={(v) => setField('city', v)}
              />
              <TextInput
                style={[styles.input, styles.half]}
                placeholder="State *"
                value={shipment.state}
                onChangeText={(v) => setField('state', v)}
              />
            </View>
            <View style={styles.row}>
              <TextInput
                style={[styles.input, styles.half]}
                placeholder="ZIP / postal *"
                value={shipment.zipCode}
                onChangeText={(v) => setField('zipCode', v)}
              />
              <TextInput
                style={[styles.input, styles.half]}
                placeholder="Country *"
                value={shipment.country}
                onChangeText={(v) => setField('country', v)}
              />
            </View>
            <TextInput
              style={styles.input}
              placeholder="Phone"
              value={shipment.phone}
              onChangeText={(v) => setField('phone', v)}
            />

            <Text style={[styles.sectionTitle, styles.mt]}>Coupon</Text>
            <TextInput
              style={styles.input}
              placeholder="e.g. WELCOME10"
              autoCapitalize="characters"
              value={couponCode}
              onChangeText={setCouponCode}
            />

            <Text style={[styles.sectionTitle, styles.mt]}>Order summary</Text>
            <View style={styles.summaryCard}>
              {cart.items.map((item) => (
                <View key={item.cartItemId} style={styles.summaryLine}>
                  <Text style={styles.summaryName}>
                    {item.productName} × {item.quantity}
                  </Text>
                  <Text style={styles.summaryPrice}>{money(item.subtotal)}</Text>
                </View>
              ))}
              <View style={styles.divider} />
              <View style={styles.summaryLine}>
                <Text style={styles.summaryName}>Total</Text>
                <Text style={styles.summaryTotal}>{money(total)}</Text>
              </View>
              <Text style={styles.hint}>Coupon discount is applied automatically at checkout.</Text>
            </View>

            {error ? <Text style={styles.error}>{error}</Text> : null}

            <Pressable style={[styles.primaryBtn, submitting && styles.disabled]} onPress={placeOrder} disabled={submitting}>
              <Text style={styles.primaryBtnText}>{submitting ? 'Placing order…' : 'Place order & pay'}</Text>
            </Pressable>
          </ScrollView>
        )}
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#f3f4f6' },
  flex: { flex: 1 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  empty: { color: '#6b7280', fontSize: 15 },
  content: { padding: 20, gap: 10 },
  sectionTitle: { fontSize: 16, fontWeight: '800', color: '#111827', marginTop: 4 },
  mt: { marginTop: 12 },
  row: { flexDirection: 'row', gap: 10 },
  half: { flex: 1 },
  input: {
    backgroundColor: '#ffffff',
    borderRadius: 8,
    paddingHorizontal: 14,
    paddingVertical: 11,
    fontSize: 15,
  },
  summaryCard: { backgroundColor: '#ffffff', borderRadius: 12, padding: 14, gap: 8 },
  summaryLine: { flexDirection: 'row', justifyContent: 'space-between', gap: 12 },
  summaryName: { flex: 1, color: '#374151', fontSize: 14 },
  summaryPrice: { color: '#111827', fontSize: 14, fontWeight: '600' },
  divider: { height: StyleSheet.hairlineWidth, backgroundColor: '#d1d5db' },
  summaryTotal: { color: '#111827', fontSize: 18, fontWeight: '800' },
  hint: { color: '#6b7280', fontSize: 12 },
  error: { color: '#b91c1c', fontSize: 14, textAlign: 'center' },
  primaryBtn: { backgroundColor: '#2563eb', borderRadius: 8, paddingVertical: 14, alignItems: 'center', marginTop: 8 },
  primaryBtnText: { color: '#ffffff', fontSize: 16, fontWeight: '700' },
  disabled: { opacity: 0.6 },
  successBody: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 8, padding: 24 },
  successTitle: { fontSize: 24, fontWeight: '800', color: '#047857' },
  successSub: { fontSize: 16, color: '#374151' },
  successTotal: { fontSize: 30, fontWeight: '800', color: '#111827' },
  successDiscount: { color: '#047857', fontSize: 14 },
  secondaryLink: { color: '#2563eb', fontSize: 15, fontWeight: '600', padding: 12 },
});