import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import type { Product, ProductReviewsResponse } from '@/lib/types';

const money = (v: number) => `$${v.toFixed(2)}`;

function Stars({ rating }: { rating: number }) {
  return <Text style={styles.stars}>{'★'.repeat(Math.round(rating))}{'☆'.repeat(5 - Math.round(rating))}</Text>;
}

export default function ProductScreen() {
  const { slug } = useLocalSearchParams<{ slug: string }>();
  const router = useRouter();
  const { token } = useAuth();

  const [product, setProduct] = useState<Product | null>(null);
  const [reviews, setReviews] = useState<ProductReviewsResponse | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    if (!slug) return;
    let cancelled = false;
    setLoading(true);
    api<Product>(`/api/products/${slug}`)
      .then((p) => {
        if (cancelled) return;
        setProduct(p);
        return api<ProductReviewsResponse>(`/api/products/${p.id}/reviews`).then((r) => {
          if (!cancelled) setReviews(r);
        });
      })
      .catch((e) => {
        if (!cancelled) setNotice(e instanceof Error ? e.message : 'Failed to load product');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [slug]);

  async function addToCart() {
    if (!product || busy) return;
    setBusy(true);
    setNotice(null);
    try {
      await api('/api/cart/items', { method: 'POST', body: { productId: product.id, quantity }, token });
      setNotice(`Added ${quantity} × ${product.name} to cart`);
    } catch (e) {
      setNotice(e instanceof Error ? e.message : 'Failed to add to cart');
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (!product) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorMessage}>{notice ?? 'Product not found'}</Text>
      </View>
    );
  }

  const maxQty = Math.min(product.stockQuantity || 1, 99);

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <Text style={styles.name}>{product.name}</Text>
      <Text style={styles.meta}>
        {product.categoryName} · {product.sku}
      </Text>
      <Text style={styles.price}>{money(product.price)}</Text>
      <Text style={[styles.stock, product.stockQuantity === 0 && styles.stockOut]}>
        {product.stockQuantity > 0 ? `${product.stockQuantity} in stock` : 'Out of stock'}
      </Text>

      {product.description ? <Text style={styles.description}>{product.description}</Text> : null}

      {product.stockQuantity > 0 ? (
        <View style={styles.addRow}>
          <View style={styles.stepper}>
            <Pressable
              style={styles.stepBtn}
              onPress={() => setQuantity((q) => Math.max(1, q - 1))}
              disabled={quantity <= 1}
            >
              <Text style={styles.stepText}>−</Text>
            </Pressable>
            <Text style={styles.qtyText}>{quantity}</Text>
            <Pressable
              style={styles.stepBtn}
              onPress={() => setQuantity((q) => Math.min(maxQty, q + 1))}
              disabled={quantity >= maxQty}
            >
              <Text style={styles.stepText}>+</Text>
            </Pressable>
          </View>
          <Pressable style={[styles.addBtn, busy && styles.disabled]} onPress={addToCart} disabled={busy}>
            <Text style={styles.addBtnText}>{busy ? 'Adding…' : 'Add to cart'}</Text>
          </Pressable>
        </View>
      ) : null}

      {notice ? <Text style={styles.notice}>{notice}</Text> : null}

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Reviews</Text>
        {reviews && reviews.totalReviews > 0 ? (
          <>
            <Text style={styles.ratingLine}>
              <Stars rating={reviews.averageRating} /> {reviews.averageRating.toFixed(1)} ·{' '}
              {reviews.totalReviews} review{reviews.totalReviews === 1 ? '' : 's'}
            </Text>
            {reviews.reviews.map((r) => (
              <View key={r.reviewId} style={styles.reviewCard}>
                <Text style={styles.reviewUser}>
                  {r.userFullName} · <Stars rating={r.rating} />
                </Text>
                {r.comment ? <Text style={styles.reviewComment}>{r.comment}</Text> : null}
              </View>
            ))}
          </>
        ) : (
          <Text style={styles.noReviews}>No reviews yet. Purchase the product to write the first one.</Text>
        )}
      </View>

      <Pressable onPress={() => router.push('/cart')} style={styles.goCart}>
        <Text style={styles.goCartText}>Go to cart →</Text>
      </Pressable>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#f3f4f6' },
  content: { padding: 20, gap: 12 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  name: { fontSize: 24, fontWeight: '800', color: '#111827' },
  meta: { fontSize: 13, color: '#6b7280' },
  price: { fontSize: 24, fontWeight: '800', color: '#2563eb' },
  stock: { fontSize: 14, color: '#047857', fontWeight: '600' },
  stockOut: { color: '#b91c1c' },
  description: { fontSize: 15, color: '#374151', lineHeight: 22 },
  addRow: { flexDirection: 'row', alignItems: 'center', gap: 12, marginTop: 4 },
  stepper: { flexDirection: 'row', alignItems: 'center', borderWidth: 1, borderColor: '#d1d5db', borderRadius: 8 },
  stepBtn: { paddingHorizontal: 12, paddingVertical: 8 },
  stepText: { fontSize: 18, fontWeight: '700', color: '#374151' },
  qtyText: { fontSize: 16, fontWeight: '700', paddingHorizontal: 8, minWidth: 28, textAlign: 'center' },
  addBtn: { flex: 1, backgroundColor: '#2563eb', borderRadius: 8, paddingVertical: 10, alignItems: 'center' },
  addBtnText: { color: '#ffffff', fontSize: 16, fontWeight: '700' },
  disabled: { opacity: 0.6 },
  notice: { color: '#047857', fontSize: 14, fontWeight: '600' },
  section: { marginTop: 16, gap: 10 },
  sectionTitle: { fontSize: 18, fontWeight: '800', color: '#111827' },
  ratingLine: { fontSize: 15, color: '#374151' },
  stars: { color: '#f59e0b', fontSize: 14 },
  reviewCard: { backgroundColor: '#ffffff', borderRadius: 10, padding: 12, gap: 4 },
  reviewUser: { fontSize: 13, fontWeight: '700', color: '#374151' },
  reviewComment: { fontSize: 14, color: '#111827' },
  noReviews: { color: '#6b7280', fontSize: 14 },
  goCart: { marginTop: 16, alignSelf: 'center' },
  goCartText: { color: '#2563eb', fontSize: 15, fontWeight: '600' },
  errorMessage: { color: '#b91c1c', fontSize: 15 },
});