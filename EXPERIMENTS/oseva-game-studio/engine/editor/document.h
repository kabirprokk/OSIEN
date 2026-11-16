// OGS Engine | version 0.1.0 | Editable project document
#pragma once

#include "core/types.h"

#include <cstdint>
#include <string>
#include <vector>

namespace ogs::editor {

enum class ObjectKind : std::uint8_t {
    Orb,       // player body (spawn)
    Platform,  // Neon slab / Sling bouncy wall (mover_amp/spd animates it)
    Planet,    // gravity well (mass + optional roam orbit)
    Hazard,    // spikes (deadly)
    Goal,      // gold goal ring (Sling)
    Coin,      // gold pickup (value 1, big 5 when scale > 1.5)
    Mote,      // light orb pickup (+10, Neon)
    Pad,       // sling launch pad (Sling)
    Gate       // golden finish gate (Neon level end)
};
enum class GameType : std::uint8_t { Platformer, TopDown, OrbitPuzzle, SideScroller };

struct SceneObject {
    std::uint32_t id = 0;
    ObjectKind kind = ObjectKind::Orb;
    std::string name;
    Vec2 position{};
    float scale = 1.0F;
    // Physics extras (Neon Pulse / Orbit Sling primitives). Ignored by kinds that don't use them.
    float mass = 0.0F;        // Planet well strength (100..1500, default 420)
    float orbit_rad = 0.0F;   // Planet roam radius (0 = static, 10..120)
    float orbit_spd = 0.0F;   // Planet roam speed (-2..2)
    float mover_amp = 0.0F;   // Platform sine amplitude px (0 = static)
    float mover_spd = 0.0F;   // Platform sine speed
    int value = 0;            // Coin pickup value override (0 = auto by scale)
};

class Document {
public:
    std::uint32_t add(ObjectKind kind, Vec2 position);
    bool erase(std::uint32_t id);
    bool move(std::uint32_t id, Vec2 position);
    bool set_scale(std::uint32_t id, float scale);
    bool rename(std::uint32_t id, std::string name);
    bool set_player_spawn(std::uint32_t id);
    bool set_game_type(GameType type);
    bool set_background_color(std::uint32_t rgb);
    bool set_mass(std::uint32_t id, float mass);
    bool set_orbit(std::uint32_t id, float radius, float speed);
    bool set_mover(std::uint32_t id, float amplitude, float speed);
    bool set_value(std::uint32_t id, int value);
    // One-click power presets (wraps set_mass/set_orbit/set_mover). Returns a
    // short label for the status bar, or nullptr when the kind has no action.
    const char* cycle_object_action(std::uint32_t id);
    bool set_par(int par);
    bool set_hint(std::string hint);
    std::uint32_t duplicate(std::uint32_t id);
    void clear();
    void begin_edit();

    bool undo();
    bool redo();
    [[nodiscard]] const std::vector<SceneObject>& objects() const noexcept;
    [[nodiscard]] SceneObject* find(std::uint32_t id) noexcept;
    [[nodiscard]] const SceneObject* find(std::uint32_t id) const noexcept;
    [[nodiscard]] std::uint32_t selected_id() const noexcept;
    [[nodiscard]] std::uint32_t player_spawn_id() const noexcept;
    [[nodiscard]] GameType game_type() const noexcept;
    [[nodiscard]] std::uint32_t background_color() const noexcept;
    [[nodiscard]] int par() const noexcept;
    [[nodiscard]] const std::string& hint() const noexcept;
    [[nodiscard]] const char* object_action_label(std::uint32_t id) const;
    [[nodiscard]] bool is_player_spawn(std::uint32_t id) const noexcept;
    void select(std::uint32_t id) noexcept;

    bool save(const std::wstring& path) const;
    bool load(const std::wstring& path);

private:
    struct Snapshot {
        std::vector<SceneObject> objects;
        std::uint32_t next_id = 1;
        std::uint32_t selected_id = 0;
        std::uint32_t player_spawn_id = 0;
        GameType game_type = GameType::Platformer;
        std::uint32_t background_color = 0x0B0E16;
        int par = 3;
        std::string hint = "Reach the golden goal.";
    };

    void checkpoint();
    Snapshot snapshot() const;
    void restore(Snapshot state);

    std::vector<SceneObject> objects_;
    std::vector<Snapshot> undo_;
    std::vector<Snapshot> redo_;
    std::uint32_t next_id_ = 1;
    std::uint32_t selected_id_ = 0;
    std::uint32_t player_spawn_id_ = 0;
    GameType game_type_ = GameType::Platformer;
    std::uint32_t background_color_ = 0x0B0E16;
    int par_ = 3;
    std::string hint_ = "Reach the golden goal.";
};

} // namespace ogs::editor
