// OGS Engine | version 0.1.0 | Core value types
#pragma once

#include <cstdint>

namespace ogs {

struct Vec2 {
    float x = 0.0F;
    float y = 0.0F;

    constexpr Vec2 operator+(const Vec2 other) const noexcept {
        return {x + other.x, y + other.y};
    }

    constexpr Vec2 operator-(const Vec2 other) const noexcept {
        return {x - other.x, y - other.y};
    }

    constexpr Vec2 operator*(const float scalar) const noexcept {
        return {x * scalar, y * scalar};
    }

    constexpr Vec2& operator+=(const Vec2 other) noexcept {
        x += other.x;
        y += other.y;
        return *this;
    }

    constexpr Vec2& operator*=(const float scalar) noexcept {
        x *= scalar;
        y *= scalar;
        return *this;
    }
};

constexpr Vec2 operator*(const float scalar, const Vec2 value) noexcept {
    return value * scalar;
}

struct Color {
    float r = 1.0F;
    float g = 1.0F;
    float b = 1.0F;
    float a = 1.0F;
};

struct LogicalSize {
    std::int32_t width = 960;
    std::int32_t height = 540;
};

} // namespace ogs

