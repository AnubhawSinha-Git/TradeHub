import { useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AccountBar } from '@/components/account-bar';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import type { Category, PageResponse, Product } from '@/lib/types';

const money = (v: number) => `$${v.toFixed(2)}`;

export default function CatalogScreen() {
  const router = useRouter();
  const { token } = useAuth();

  const [categories, setCategories] = useState<Category[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [keyword, setKeyword] = useState('');
  const [categorySlug, setCategorySlug] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<Category[]>('/api/categories')
      .then(setCategories)
      .catch(() => setCategories([]));
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    const params = new URLSearchParams({ page: '0', size: '100' });
    if (keyword.trim()) params.set('keyword', keyword.trim());
    if (categorySlug) params.set('category', categorySlug);

    api<PageResponse<Product>>(`/api/products?${params.toString()}`)
      .then((page) => {
        if (!cancelled) setProducts(page.content);
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Failed to load products');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [keyword, categorySlug]);

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <AccountBar />
      <View style={styles.searchRow}>
        <TextInput
          style={styles.search}
          placeholder="Search products…"
          placeholderTextColor="#9ca3af"
          value={keyword}
          onChangeText={setKeyword}
          autoCapitalize="none"
        />
      </View>

      <FlatList
        horizontal
        style={styles.chipList}
        contentContainerStyle={styles.chipContent}
        data={[{ id: 0, name: 'All', slug: '' }, ...categories]}
        keyExtractor={(c) => c.slug || 'all'}
        renderItem={({ item }) => {
          const active = (item.slug || '') === (categorySlug ?? '');
          return (
            <Pressable
              onPress={() => setCategorySlug(item.slug || null)}
              style={[styles.chip, active && styles.chipActive]}
            >
              <Text style={[styles.chipText, active && styles.chipTextActive]}>{item.name}</Text>
            </Pressable>
          );
        }}
      />

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" />
        </View>
      ) : error ? (
        <View style={styles.center}>
          <Text style={styles.error}>{error}</Text>
        </View>
      ) : products.length === 0 ? (
        <View style={styles.center}>
          <Text style={styles.empty}>No products found</Text>
        </View>
      ) : (
        <FlatList
          data={products}
          keyExtractor={(p) => String(p.id)}
          contentContainerStyle={styles.list}
          renderItem={({ item }) => (
            <Pressable style={styles.card} onPress={() => router.push(`/product/${item.slug}`)}>
              <Text style={styles.cardName}>{item.name}</Text>
              <Text style={styles.cardMeta}>
                {item.categoryName} · {item.sku}
              </Text>
              <View style={styles.cardBottom}>
                <Text style={styles.price}>{money(item.price)}</Text>
                <Text style={[styles.stock, item.stockQuantity === 0 && styles.stockOut]}>
                  {item.stockQuantity > 0 ? `${item.stockQuantity} in stock` : 'Out of stock'}
                </Text>
              </View>
            </Pressable>
          )}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#f3f4f6' },
  searchRow: { paddingHorizontal: 16, paddingTop: 12, backgroundColor: '#ffffff' },
  search: {
    backgroundColor: '#f3f4f6',
    borderRadius: 8,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 15,
  },
  chipList: { backgroundColor: '#ffffff', flexGrow: 0 },
  chipContent: { paddingHorizontal: 16, paddingVertical: 12, gap: 8 },
  chip: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 20,
    paddingHorizontal: 14,
    paddingVertical: 6,
  },
  chipActive: { backgroundColor: '#2563eb', borderColor: '#2563eb' },
  chipText: { color: '#374151', fontSize: 14 },
  chipTextActive: { color: '#ffffff', fontWeight: '600' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  error: { color: '#b91c1c', fontSize: 15 },
  empty: { color: '#6b7280', fontSize: 15 },
  list: { padding: 16, gap: 12 },
  card: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 16,
    gap: 6,
  },
  cardName: { fontSize: 16, fontWeight: '700', color: '#111827' },
  cardMeta: { fontSize: 13, color: '#6b7280' },
  cardBottom: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 },
  price: { fontSize: 17, fontWeight: '800', color: '#2563eb' },
  stock: { fontSize: 13, color: '#047857' },
  stockOut: { color: '#b91c1c' },
});