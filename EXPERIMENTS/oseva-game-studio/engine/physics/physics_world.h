// OGS Engine | version 0.2.0 | Deterministic 2D force world
#pragma once

#include "core/types.h"

#include <cstdint>
#include <vector>

namespace ogs::physics {

using BodyId = std::uint32_t;

struct Body {
    BodyId id = 0;
    Vec2 position{};
    Vec2 velocity{};
    float radius = 12.0F;
    float mass = 1.0F;
    float gravity_scale = 1.0F;
    bool dynamic = true;
};

struct GravityWell {
    Vec2 position{};
    float strength = 900.0F;
    float influence_radius = 500.0F;
    bool invert = false;
};

class PhysicsWorld {
public:
    BodyId create_body(const Body& body = {});
    void add_gravity_well(GravityWell well);
    void clear_gravity_wells();
    void step(float fixed_delta_seconds);

    [[nodiscard]] const Body* body(BodyId id) const noexcept;
    [[nodiscard]] Body* body(BodyId id) noexcept;
    [[nodiscard]] const std::vector<Body>& bodies() const noexcept;

private:
    std::vector<Body> bodies_;
    std::vector<GravityWell> wells_;
    BodyId next_id_ = 1;
};

} // namespace ogs::physics
