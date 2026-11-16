// OGS Engine | version 0.2.0 | Runtime sprite identity contract
#pragma once

#include "core/types.h"

#include <cmath>

namespace ogs::renderer {

struct SpriteStyle {
    Color core{0.019F, 0.024F, 0.059F, 1.0F};
    Color center{0.933F, 0.949F, 1.0F, 1.0F};
    Color aura{0.0F, 0.941F, 1.0F, 1.0F};
    Color ring{0.933F, 0.949F, 1.0F, 1.0F};
    float ring_width = 1.6F;
    float aura_strength = 0.85F;
    float breathe_speed = 3.2F;
    float breathe_amount = 0.04F;
};

struct SpritePose {
    Vec2 position{};
    float rotation = 0.0F;
    float scale = 1.0F;
    float wobble = 0.0F;
};

[[nodiscard]] inline float breathe_scale(const float time_seconds, const SpriteStyle& style = {}) noexcept {
    return 1.0F + std::sin(time_seconds * style.breathe_speed) * style.breathe_amount;
}

} // namespace ogs::renderer
