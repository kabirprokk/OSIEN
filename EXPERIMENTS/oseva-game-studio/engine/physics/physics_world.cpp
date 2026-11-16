// OGS Engine | version 0.2.0 | Deterministic 2D force world
#include "physics/physics_world.h"

#include <cmath>
#include <algorithm>

namespace ogs::physics {
namespace {

constexpr float kEpsilon = 0.0001F;

} // namespace

BodyId PhysicsWorld::create_body(const Body& body_value) {
    Body body = body_value;
    body.id = next_id_++;
    bodies_.push_back(body);
    return body.id;
}

void PhysicsWorld::add_gravity_well(const GravityWell well) {
    wells_.push_back(well);
}

void PhysicsWorld::clear_gravity_wells() {
    wells_.clear();
}

void PhysicsWorld::step(const float fixed_delta_seconds) {
    if (!std::isfinite(fixed_delta_seconds) || fixed_delta_seconds <= 0.0F) {
        return;
    }
    for (Body& body : bodies_) {
        if (!body.dynamic || !std::isfinite(body.position.x) || !std::isfinite(body.position.y) ||
            !std::isfinite(body.velocity.x) || !std::isfinite(body.velocity.y) ||
            !std::isfinite(body.gravity_scale) || !std::isfinite(body.radius)) {
            continue;
        }

        Vec2 acceleration{};
        for (const GravityWell& well : wells_) {
            const Vec2 delta = well.position - body.position;
            const float raw_distance_squared = delta.x * delta.x + delta.y * delta.y;
            if (!std::isfinite(raw_distance_squared) || raw_distance_squared <= kEpsilon ||
                !std::isfinite(well.strength) || !std::isfinite(well.influence_radius) ||
                well.influence_radius <= 0.0F) {
                continue;
            }

            const float distance = std::sqrt(raw_distance_squared);
            if (distance > well.influence_radius) {
                continue;
            }

            const float direction = well.invert ? -1.0F : 1.0F;
            const float softened_distance_squared = std::max(raw_distance_squared,
                std::max(body.radius * body.radius, kEpsilon));
            const float force = direction * well.strength / softened_distance_squared;
            acceleration += (delta * (force / distance)) * body.gravity_scale;
        }

        body.velocity += acceleration * fixed_delta_seconds;
        body.position += body.velocity * fixed_delta_seconds;
        if (!std::isfinite(body.position.x) || !std::isfinite(body.position.y) ||
            !std::isfinite(body.velocity.x) || !std::isfinite(body.velocity.y)) {
            body.position = {};
            body.velocity = {};
        }
    }
}

const Body* PhysicsWorld::body(const BodyId id) const noexcept {
    for (const Body& value : bodies_) {
        if (value.id == id) {
            return &value;
        }
    }
    return nullptr;
}

Body* PhysicsWorld::body(const BodyId id) noexcept {
    for (Body& value : bodies_) {
        if (value.id == id) {
            return &value;
        }
    }
    return nullptr;
}

const std::vector<Body>& PhysicsWorld::bodies() const noexcept {
    return bodies_;
}

} // namespace ogs::physics
