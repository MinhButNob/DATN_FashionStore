import { defineStore } from "pinia";
import { ref, computed } from "vue";

export const useAuthStore = defineStore("auth", () => {
  // State
  const user = ref(null);
  const token = ref(null);
  const isAuthenticated = ref(false);

  // Load from localStorage
  const loadAuth = () => {
    const savedUser = localStorage.getItem("user");
    const savedToken = localStorage.getItem("token");

    if (savedUser && savedToken) {
      try {
        user.value = JSON.parse(savedUser);
        token.value = savedToken;
        isAuthenticated.value = true;
      } catch (e) {
        console.error("Failed to parse user data");
        logout();
      }
    }
  };

  // Initialize
  loadAuth();

  // Getters
  const isAdmin = computed(() => {
    return user.value?.role === "admin";
  });

  const userName = computed(() => {
    return user.value?.name || "Khách";
  });

  const userEmail = computed(() => {
    return user.value?.email || "";
  });

  // Actions
  function login(email, password) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        // Mock validation
        if (!email || !password) {
          reject(new Error("Vui lòng nhập email và mật khẩu"));
          return;
        }

        // Check if email exists in our mock database
        const mockUsers = [
          {
            id: 1,
            name: "Quản trị viên",
            email: "admin@gmail.com",
            password: "12345678",
            role: "admin",
            phone: "0909123456",
            address: "123 Nguyễn Huệ, Q.1, TP.HCM",
          },
          {
            id: 2,
            name: "Nhân viên",
            email: "nhanvien@gmail.com",
            password: "12345678",
            role: "staff",
            phone: "0987654321",
            address: "456 Lê Lợi, Q.1, TP.HCM",
          },
          {
            id: 3,
            name: "Khách hàng",
            email: "khachhang@gmail.com",
            password: "12345678",
            role: "customer",
            phone: "0123456789",
            address: "789 Trần Hưng Đạo, Q.1, TP.HCM",
          },
        ];

        // Also check in registeredUsers from localStorage(
        let foundUser = mockUsers.find(
          (u) => u.email === email && u.password === password,
        );

        // If not found in mock, try to find in localStorage (registered users)
        if (!foundUser) {
          const registeredUsers = JSON.parse(
            localStorage.getItem("registeredUsers") || "[]",
          );
          foundUser = registeredUsers.find(
            (u) => u.email === email && u.password === password,
          );
        }

        if (foundUser) {
          // Remove password before storing
          const { password, ...userData } = foundUser;

          user.value = userData;
          token.value = "fake-jwt-token-" + Date.now();
          isAuthenticated.value = true;

          localStorage.setItem("user", JSON.stringify(userData));
          localStorage.setItem("token", token.value);

          resolve(userData);
        } else {
          reject(new Error("Email hoặc mật khẩu không đúng"));
        }
      }, 1000);
    });
  }

  function register(userData) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        // Validate
        if (!userData.name || !userData.email || !userData.password) {
          reject(new Error("Vui lòng điền đầy đủ thông tin"));
          return;
        }

        // Check if email already exists
        const existingUsers = JSON.parse(
          localStorage.getItem("registeredUsers") || "[]",
        );
        const mockEmails = [
          "admin@gmail.com",
          "nhanvien@gmail.com",
          "khachhang@gmail.com",
        ];

        if (
          mockEmails.includes(userData.email) ||
          existingUsers.some((u) => u.email === userData.email)
        ) {
          reject(new Error("Email đã được đăng ký"));
          return;
        }

        // Create new user
        const newUser = {
          id: Date.now(),
          name: userData.name,
          email: userData.email,
          phone: userData.phone || "",
          role: "customer",
          created_at: new Date().toISOString(),
        };

        // Save to localStorage (simulate database)
        existingUsers.push({
          ...newUser,
          password: userData.password,
        });
        localStorage.setItem("registeredUsers", JSON.stringify(existingUsers));

        // Auto login after registration
        user.value = newUser;
        token.value = "fake-jwt-token-" + Date.now();
        isAuthenticated.value = true;

        localStorage.setItem("user", JSON.stringify(newUser));
        localStorage.setItem("token", token.value);

        resolve(newUser);
      }, 1500);
    });
  }

  function logout() {
    user.value = null;
    token.value = null;
    isAuthenticated.value = false;

    localStorage.removeItem("user");
    localStorage.removeItem("token");
    // Don't remove registered users data
  }

  function updateProfile(profileData) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        try {
          const updatedUser = { ...user.value, ...profileData };
          user.value = updatedUser;
          localStorage.setItem("user", JSON.stringify(updatedUser));
          resolve(updatedUser);
        } catch (error) {
          reject(new Error("Cập nhật thất bại"));
        }
      }, 1000);
    });
  }

  function changePassword(currentPassword, newPassword) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        // Mock password change
        if (
          currentPassword === "password123" ||
          currentPassword === "admin123"
        ) {
          resolve({ success: true });
        } else {
          reject(new Error("Mật khẩu hiện tại không đúng"));
        }
      }, 1000);
    });
  }

  function forgotPassword(email) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        // Check if email exists
        const mockEmails = [
          "user@example.com",
          "admin@fashionstore.vn",
          "customer@test.com",
        ];
        const registeredUsers = JSON.parse(
          localStorage.getItem("registeredUsers") || "[]",
        );
        const allEmails = [
          ...mockEmails,
          ...registeredUsers.map((u) => u.email),
        ];

        if (allEmails.includes(email)) {
          resolve({ success: true, message: "Email đã được gửi" });
        } else {
          reject(new Error("Email không tồn tại trong hệ thống"));
        }
      }, 1500);
    });
  }

  function resetPassword(token, newPassword) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        // Mock token validation
        if (token === "valid-token" || token.length > 10) {
          resolve({ success: true });
        } else {
          reject(new Error("Token không hợp lệ hoặc đã hết hạn"));
        }
      }, 1500);
    });
  }

  return {
    // State
    user,
    token,
    isAuthenticated,

    // Getters
    isAdmin,
    userName,
    userEmail,

    // Actions
    login,
    register,
    logout,
    updateProfile,
    changePassword,
    forgotPassword,
    resetPassword,
  };
});
