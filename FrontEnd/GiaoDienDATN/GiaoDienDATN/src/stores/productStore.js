import { defineStore } from "pinia";
import { ref, computed } from "vue";

export const useProductStore = defineStore("products", () => {
  // State
  const products = ref([
    {
      id: 1,
      name: "Áo thun nam cổ tròn",
      category: "men",
      price: 199000,
      oldPrice: 299000,
      rating: 4.5,
      sold: 120,
      stock: 50,
      image: "tshirt",
      tag: "Hot",
      status: "active",
      description:
        "Áo thun nam chất liệu cotton cao cấp, thoáng mát, thấm hút mồ hôi tốt.",
    },
    {
      id: 2,
      name: "Đầm nữ công sở",
      category: "women",
      price: 399000,
      oldPrice: 599000,
      rating: 4.8,
      sold: 85,
      stock: 30,
      image: "dress",
      tag: "Sale",
      status: "active",
      description:
        "Đầm công sở thiết kế thanh lịch, chất liệu cao cấp không nhăn.",
    },
    {
      id: 3,
      name: "Quần jean nam",
      category: "men",
      price: 499000,
      oldPrice: 699000,
      rating: 4.6,
      sold: 200,
      stock: 100,
      image: "jeans",
      tag: "Mới",
      status: "active",
      description: "Quần jean nam form regular, chất liệu denim cao cấp.",
    },
    {
      id: 4,
      name: "Áo khoác nữ",
      category: "women",
      price: 599000,
      oldPrice: 899000,
      rating: 4.7,
      sold: 65,
      stock: 25,
      image: "jacket",
      tag: "Hot",
      status: "active",
      description: "Áo khoác nữ thời trang, chất liệu gió chống nước.",
    },
    {
      id: 5,
      name: "Quần short trẻ em",
      category: "kids",
      price: 149000,
      oldPrice: 249000,
      rating: 4.4,
      sold: 45,
      stock: 80,
      image: "shorts",
      tag: "",
      status: "active",
      description: "Quần short trẻ em thoáng mát, phù hợp cho bé vận động.",
    },
    {
      id: 6,
      name: "Túi xách nữ",
      category: "accessories",
      price: 299000,
      oldPrice: 399000,
      rating: 4.3,
      sold: 30,
      stock: 40,
      image: "bag",
      tag: "",
      status: "active",
      description: "Túi xách nữ thời trang, chất liệu da cao cấp.",
    },
  ]);

  const categories = ref([
    { id: 1, name: "Thời trang nam", slug: "men", count: 45, status: "active" },
    {
      id: 2,
      name: "Thời trang nữ",
      slug: "women",
      count: 67,
      status: "active",
    },
    { id: 3, name: "Trẻ em", slug: "kids", count: 23, status: "active" },
    {
      id: 4,
      name: "Phụ kiện",
      slug: "accessories",
      count: 34,
      status: "active",
    },
  ]);

  const filters = ref({
    category: "all",
    search: "",
    sort: "default",
    priceRange: "all",
    tags: [],
    ratings: [],
  });

  // Getters
  const filteredProducts = computed(() => {
    let filtered = [...products.value];

    // Search
    if (filters.value.search) {
      const searchTerm = filters.value.search.toLowerCase();
      filtered = filtered.filter(
        (p) =>
          p.name.toLowerCase().includes(searchTerm) ||
          p.description?.toLowerCase().includes(searchTerm),
      );
    }

    // Category
    if (filters.value.category !== "all") {
      filtered = filtered.filter((p) => p.category === filters.value.category);
    }

    // Price range
    if (filters.value.priceRange !== "all") {
      const [min, max] = filters.value.priceRange.split("-").map(Number);
      filtered = filtered.filter((p) => {
        if (max) {
          return p.price >= min && p.price <= max;
        } else {
          return p.price >= min;
        }
      });
    }

    // Tags
    if (filters.value.tags.length > 0) {
      filtered = filtered.filter(
        (p) => p.tag && filters.value.tags.includes(p.tag),
      );
    }

    // Ratings
    if (filters.value.ratings.length > 0) {
      filtered = filtered.filter((p) =>
        filters.value.ratings.some((r) => p.rating >= r),
      );
    }

    // Sort
    switch (filters.value.sort) {
      case "price-asc":
        filtered.sort((a, b) => a.price - b.price);
        break;
      case "price-desc":
        filtered.sort((a, b) => b.price - a.price);
        break;
      case "name-asc":
        filtered.sort((a, b) => a.name.localeCompare(b.name));
        break;
      case "name-desc":
        filtered.sort((a, b) => b.name.localeCompare(a.name));
        break;
      case "rating":
        filtered.sort((a, b) => b.rating - a.rating);
        break;
      case "sold":
        filtered.sort((a, b) => b.sold - a.sold);
        break;
    }

    return filtered;
  });

  const featuredProducts = computed(() => {
    return products.value
      .filter((p) => p.tag === "Hot" || p.sold > 50)
      .slice(0, 4);
  });

  const newProducts = computed(() => {
    return products.value.filter((p) => p.tag === "Mới").slice(0, 4);
  });

  const bestSellers = computed(() => {
    return [...products.value].sort((a, b) => b.sold - a.sold).slice(0, 4);
  });

  // Actions
  function setFilter(key, value) {
    filters.value[key] = value;
  }

  function resetFilters() {
    filters.value = {
      category: "all",
      search: "",
      sort: "default",
      priceRange: "all",
      tags: [],
      ratings: [],
    };
  }

  function addProduct(product) {
    const newProduct = {
      ...product,
      id: Date.now(),
      sold: 0,
      rating: 4.5,
    };
    products.value.push(newProduct);
  }

  function updateProduct(updatedProduct) {
    const index = products.value.findIndex((p) => p.id === updatedProduct.id);
    if (index !== -1) {
      products.value[index] = updatedProduct;
    }
  }

  function deleteProduct(id) {
    const index = products.value.findIndex((p) => p.id === id);
    if (index !== -1) {
      products.value.splice(index, 1);
    }
  }

  function toggleProductStatus(id) {
    const product = products.value.find((p) => p.id === id);
    if (product) {
      product.status = product.status === "active" ? "inactive" : "active";
    }
  }

  return {
    // State
    products,
    categories,
    filters,

    // Getters
    filteredProducts,
    featuredProducts,
    newProducts,
    bestSellers,

    // Actions
    setFilter,
    resetFilters,
    addProduct,
    updateProduct,
    deleteProduct,
    toggleProductStatus,
  };
});
