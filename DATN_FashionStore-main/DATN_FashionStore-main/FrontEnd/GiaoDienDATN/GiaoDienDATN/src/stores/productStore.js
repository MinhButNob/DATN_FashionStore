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

  const images = ref([
    {
      id: 1,
      name: 'Áo thun nam cổ tròn basic',
      filename: 'ao-thun-nam-co-tron.jpg',
      url: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800',
      type: 'product',
      dimensions: '800 x 1000 px',
      size: '124 KB',
      uploadedAt: '2026-05-10',
      productsAssoc: ['Áo thun nam cổ tròn', 'BST Áo Thun Basic']
    },
    {
      id: 2,
      name: 'Đầm nữ công sở dáng A thanh lịch',
      filename: 'dam-nu-cong-so.jpg',
      url: 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=800',
      type: 'product',
      dimensions: '900 x 1200 px',
      size: '245 KB',
      uploadedAt: '2026-05-12',
      productsAssoc: ['Đầm hoa văn công sở']
    },
    {
      id: 3,
      name: 'Quần jean nam bụi bặm regular',
      filename: 'quan-jean-nam.jpg',
      url: 'https://images.unsplash.com/photo-1542272604-787c3835535d?w=800',
      type: 'product',
      dimensions: '750 x 1050 px',
      size: '189 KB',
      uploadedAt: '2026-05-14',
      productsAssoc: ['Quần jean nam rách gối']
    },
    {
      id: 4,
      name: 'Áo khoác gió nữ chống thấm nước',
      filename: 'ao-khoac-gio-nu.jpg',
      url: 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800',
      type: 'product',
      dimensions: '1200 x 1500 px',
      size: '312 KB',
      uploadedAt: '2026-05-15',
      productsAssoc: ['Áo gió dù thể thao']
    },
    {
      id: 5,
      name: 'Quần short kaki trẻ em thoáng mát',
      filename: 'quan-short-tre-em.jpg',
      url: 'https://images.unsplash.com/photo-1591195853828-11db59a44f6b?w=800',
      type: 'product',
      dimensions: '800 x 800 px',
      size: '95 KB',
      uploadedAt: '2026-05-18',
      productsAssoc: ['Bộ short thun bé trai']
    },
    {
      id: 6,
      name: 'Túi xách tay nữ da thật khóa vàng',
      filename: 'tui-xach-da-nu.jpg',
      url: 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?w=800',
      type: 'product',
      dimensions: '1000 x 1000 px',
      size: '156 KB',
      uploadedAt: '2026-05-20',
      productsAssoc: ['Túi xách cao cấp Hera']
    },
    {
      id: 7,
      name: 'Banner Quảng Cáo Mùa Hè Rực Rỡ',
      filename: 'banner-summer-sale-2026.jpg',
      url: 'https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=1200',
      type: 'banner',
      dimensions: '1920 x 800 px',
      size: '512 KB',
      uploadedAt: '2026-05-25',
      productsAssoc: ['Slider trang chủ - Banner chính']
    },
    {
      id: 8,
      name: 'Banner Khai Xuân Đón Lộc',
      filename: 'banner-new-arrival.jpg',
      url: 'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=1200',
      type: 'banner',
      dimensions: '1920 x 800 px',
      size: '489 KB',
      uploadedAt: '2026-05-26',
      productsAssoc: ['Slider trang chủ - Banner phụ 1']
    },
    {
      id: 9,
      name: 'Danh Mục Giày Sneaker Năng Động',
      filename: 'category-giay-sneaker.jpg',
      url: 'https://images.unsplash.com/photo-1549298916-b41d501d3772?w=800',
      type: 'category',
      dimensions: '600 x 600 px',
      size: '88 KB',
      uploadedAt: '2026-05-01',
      productsAssoc: ['Danh mục Giày Sneaker']
    },
    {
      id: 10,
      name: 'Ảnh Đại Diện Admin Nguyễn Hà',
      filename: 'avatar-admin-nguyen-ha.jpg',
      url: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500',
      type: 'avatar',
      dimensions: '400 x 400 px',
      size: '42 KB',
      uploadedAt: '2026-04-20',
      productsAssoc: ['Hồ sơ quản trị viên']
    }
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

  function addImage(image) {
    images.value.unshift(image);
  }

  function deleteImage(id) {
    images.value = images.value.filter((img) => img.id !== id);
  }

  return {
    // State
    products,
    categories,
    filters,
    images,

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
    addImage,
    deleteImage,
  };
});
