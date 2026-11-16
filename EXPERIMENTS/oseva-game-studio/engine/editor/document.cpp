// OGS Engine | version 0.1.0 | Editable project document
#include "editor/document.h"

#include <algorithm>
#include <cmath>
#include <fstream>
#include <filesystem>
#include <iomanip>
#include <limits>
#include <sstream>
#include <unordered_set>

namespace ogs::editor {
namespace {

const char* object_name(const ObjectKind kind) {
    switch (kind) {
        case ObjectKind::Orb: return "Orb";
        case ObjectKind::Platform: return "Platform";
        case ObjectKind::Planet: return "Planet";
        case ObjectKind::Hazard: return "Hazard";
        case ObjectKind::Goal: return "Goal";
        case ObjectKind::Coin: return "Coin";
        case ObjectKind::Mote: return "Mote";
        case ObjectKind::Pad: return "Pad";
        case ObjectKind::Gate: return "Gate";
    }
    return "Object";
}

} // namespace

void Document::checkpoint() {
    undo_.push_back(snapshot());
    if (undo_.size() > 128) {
        undo_.erase(undo_.begin());
    }
    redo_.clear();
}

Document::Snapshot Document::snapshot() const {
    return {objects_, next_id_, selected_id_, player_spawn_id_, game_type_, background_color_, par_, hint_};
}

void Document::restore(Snapshot state) {
    objects_ = std::move(state.objects);
    next_id_ = state.next_id;
    selected_id_ = state.selected_id;
    player_spawn_id_ = state.player_spawn_id;
    game_type_ = state.game_type;
    background_color_ = state.background_color;
    par_ = state.par;
    hint_ = std::move(state.hint);
}

std::uint32_t Document::add(const ObjectKind kind, const Vec2 position) {
    if (!std::isfinite(position.x) || !std::isfinite(position.y)) {
        return 0;
    }
    if (static_cast<unsigned int>(kind) > static_cast<unsigned int>(ObjectKind::Gate)) return 0;
    checkpoint();
    const auto id = next_id_++;
    SceneObject object{id, kind, object_name(kind), position, 1.0F};
    if (kind == ObjectKind::Planet) object.mass = 420.0F;
    if (kind == ObjectKind::Coin) object.value = 0;  // auto: 1, big 5 when scale > 1.5
    objects_.push_back(object);
    selected_id_ = id;
    if (kind == ObjectKind::Orb && player_spawn_id_ == 0) player_spawn_id_ = id;
    return id;
}

bool Document::erase(const std::uint32_t id) {
    const auto found = std::find_if(objects_.begin(), objects_.end(), [id](const SceneObject& object) { return object.id == id; });
    if (found == objects_.end()) {
        return false;
    }
    checkpoint();
    objects_.erase(std::find_if(objects_.begin(), objects_.end(), [id](const SceneObject& object) { return object.id == id; }));
    if (player_spawn_id_ == id) player_spawn_id_ = 0;
    selected_id_ = 0;
    return true;
}

bool Document::move(const std::uint32_t id, const Vec2 position) {
    SceneObject* object = find(id);
    if (object == nullptr || !std::isfinite(position.x) || !std::isfinite(position.y)) {
        return false;
    }
    object->position = position;
    return true;
}

bool Document::set_scale(const std::uint32_t id, const float scale) {
    SceneObject* object = find(id);
    if (object == nullptr || !std::isfinite(scale)) {
        return false;
    }
    object->scale = std::clamp(scale, 0.2F, 5.0F);
    return true;
}

bool Document::rename(const std::uint32_t id, std::string name) {
    SceneObject* object = find(id);
    if (object == nullptr || name.empty() || name.size() > 128 ||
        name.find_first_of("\r\n") != std::string::npos || name.find('\0') != std::string::npos) {
        return false;
    }
    object->name = std::move(name);
    return true;
}

bool Document::set_player_spawn(const std::uint32_t id) {
    const SceneObject* object = find(id);
    if (object == nullptr || object->kind != ObjectKind::Orb) return false;
    if (player_spawn_id_ != id) {
        checkpoint();
        player_spawn_id_ = id;
    }
    return true;
}

bool Document::set_game_type(const GameType type) {
    if (static_cast<unsigned int>(type) > static_cast<unsigned int>(GameType::SideScroller)) return false;
    if (game_type_ != type) {
        checkpoint();
        game_type_ = type;
    }
    return true;
}

bool Document::set_background_color(const std::uint32_t rgb) {
    if (rgb > 0xFFFFFFU) return false;
    if (background_color_ != rgb) {
        checkpoint();
        background_color_ = rgb;
    }
    return true;
}

bool Document::set_mass(const std::uint32_t id, const float mass) {
    SceneObject* object = find(id);
    if (object == nullptr || object->kind != ObjectKind::Planet || !std::isfinite(mass)) return false;
    object->mass = std::clamp(mass, 100.0F, 1500.0F);
    return true;
}

bool Document::set_orbit(const std::uint32_t id, const float radius, const float speed) {
    SceneObject* object = find(id);
    if (object == nullptr || object->kind != ObjectKind::Planet || !std::isfinite(radius) ||
        !std::isfinite(speed)) {
        return false;
    }
    object->orbit_rad = std::clamp(radius, 0.0F, 120.0F);
    object->orbit_spd = std::clamp(speed, -2.0F, 2.0F);
    return true;
}

bool Document::set_mover(const std::uint32_t id, const float amplitude, const float speed) {
    SceneObject* object = find(id);
    if (object == nullptr || object->kind != ObjectKind::Platform || !std::isfinite(amplitude) ||
        !std::isfinite(speed)) {
        return false;
    }
    object->mover_amp = std::clamp(amplitude, 0.0F, 120.0F);
    object->mover_spd = std::clamp(speed, 0.0F, 3.0F);
    return true;
}

bool Document::set_value(const std::uint32_t id, const int value) {
    SceneObject* object = find(id);
    if (object == nullptr || object->kind != ObjectKind::Coin) return false;
    if (value < 0 || value > 99) return false;
    object->value = value;
    return true;
}

const char* Document::object_action_label(const std::uint32_t id) const {
    const SceneObject* object = find(id);
    if (object == nullptr) return nullptr;
    if (object->kind == ObjectKind::Planet) {
        if (object->orbit_rad > 0.0F) return "ROAMING WELL";
        if (object->mass >= 900.0F) return "HEAVY WELL";
        if (object->mass <= 200.0F) return "LIGHT WELL";
        return "NORMAL WELL";
    }
    if (object->kind == ObjectKind::Platform) {
        if (object->mover_amp <= 0.0F) return "STATIC SLAB";
        if (object->mover_amp >= 45.0F) return "FAST MOVER";
        return "SLOW MOVER";
    }
    if (object->kind == ObjectKind::Coin) return object->scale > 1.5F ? "BIG COIN +5" : "COIN +1";
    return nullptr;
}

const char* Document::cycle_object_action(const std::uint32_t id) {
    SceneObject* object = find(id);
    if (object == nullptr) return nullptr;
    if (object->kind != ObjectKind::Planet && object->kind != ObjectKind::Platform &&
        object->kind != ObjectKind::Coin) {
        return nullptr;
    }
    checkpoint();
    if (object->kind == ObjectKind::Planet) {
        // static light -> normal -> heavy -> roaming -> back to light
        if (object->orbit_rad > 0.0F) {
            object->orbit_rad = 0.0F;
            object->orbit_spd = 0.0F;
            object->mass = 200.0F;
            return "Light well (pull 200).";
        }
        if (object->mass <= 200.0F) {
            object->mass = 420.0F;
            return "Normal well (pull 420).";
        }
        if (object->mass < 900.0F) {
            object->mass = 900.0F;
            return "Heavy well (pull 900).";
        }
        object->orbit_rad = 48.0F;
        object->orbit_spd = 0.6F;
        return "Roaming well — drifts like Orbit Sling level 18+.";
    }
    if (object->kind == ObjectKind::Platform) {
        // static -> slow mover -> fast mover -> static
        if (object->mover_amp <= 0.0F) {
            object->mover_amp = 30.0F;
            object->mover_spd = 1.2F;
            return "Slow mover slab (Neon level 8+).";
        }
        if (object->mover_amp < 45.0F) {
            object->mover_amp = 55.0F;
            object->mover_spd = 2.0F;
            return "Fast mover slab.";
        }
        object->mover_amp = 0.0F;
        object->mover_spd = 0.0F;
        return "Static slab.";
    }
    if (object->kind == ObjectKind::Coin) {
        object->scale = object->scale > 1.5F ? 1.0F : 2.0F;
        return object->scale > 1.5F ? "Big coin (+5)." : "Coin (+1).";
    }
    return nullptr;
}

bool Document::set_par(const int par) {
    if (par < 1 || par > 9) return false;
    if (par_ != par) {
        checkpoint();
        par_ = par;
    }
    return true;
}

bool Document::set_hint(std::string hint) {
    if (hint.size() > 70) hint.resize(70);
    if (hint.find_first_of("\r\n") != std::string::npos || hint.find('\0') != std::string::npos) return false;
    if (hint.empty()) return false;
    if (hint_ != hint) {
        checkpoint();
        hint_ = std::move(hint);
    }
    return true;
}

std::uint32_t Document::duplicate(const std::uint32_t id) {
    const SceneObject* source = find(id);
    if (source == nullptr) {
        return 0;
    }
    checkpoint();
    SceneObject copy = *source;
    copy.id = next_id_++;
    copy.name += " Copy";
    copy.position.x += 28.0F;
    copy.position.y += 28.0F;
    objects_.push_back(copy);
    selected_id_ = copy.id;
    return copy.id;
}

void Document::clear() {
    checkpoint();
    objects_.clear();
    selected_id_ = 0;
    player_spawn_id_ = 0;
    next_id_ = 1;
}

void Document::begin_edit() { checkpoint(); }

bool Document::undo() {
    if (undo_.empty()) return false;
    redo_.push_back(snapshot());
    Snapshot state = std::move(undo_.back());
    undo_.pop_back();
    restore(std::move(state));
    return true;
}

bool Document::redo() {
    if (redo_.empty()) return false;
    undo_.push_back(snapshot());
    Snapshot state = std::move(redo_.back());
    redo_.pop_back();
    restore(std::move(state));
    return true;
}

const std::vector<SceneObject>& Document::objects() const noexcept { return objects_; }
SceneObject* Document::find(const std::uint32_t id) noexcept {
    for (SceneObject& object : objects_) if (object.id == id) return &object;
    return nullptr;
}
const SceneObject* Document::find(const std::uint32_t id) const noexcept {
    for (const SceneObject& object : objects_) if (object.id == id) return &object;
    return nullptr;
}
std::uint32_t Document::selected_id() const noexcept { return selected_id_; }
std::uint32_t Document::player_spawn_id() const noexcept { return player_spawn_id_; }
GameType Document::game_type() const noexcept { return game_type_; }
std::uint32_t Document::background_color() const noexcept { return background_color_; }
int Document::par() const noexcept { return par_; }
const std::string& Document::hint() const noexcept { return hint_; }
bool Document::is_player_spawn(const std::uint32_t id) const noexcept { return player_spawn_id_ == id && id != 0; }
void Document::select(const std::uint32_t id) noexcept { selected_id_ = find(id) != nullptr ? id : 0; }

bool Document::save(const std::wstring& path) const {
    std::ofstream output(std::filesystem::path(path), std::ios::binary | std::ios::trunc);
    if (!output) return false;
    output << "OGS_PROJECT 3\n" << objects_.size() << ' ' << player_spawn_id_ << ' '
           << static_cast<unsigned int>(game_type_) << ' ' << background_color_ << ' ' << par_ << ' '
           << std::quoted(hint_) << '\n';
    for (const SceneObject& object : objects_) {
        output << object.id << ' ' << static_cast<unsigned int>(object.kind) << ' '
               << std::setprecision(std::numeric_limits<float>::max_digits10)
               << object.position.x << ' ' << object.position.y << ' ' << object.scale << ' '
               << object.mass << ' ' << object.orbit_rad << ' ' << object.orbit_spd << ' '
               << object.mover_amp << ' ' << object.mover_spd << ' ' << object.value << ' '
               << std::quoted(object.name) << '\n';
    }
    return output.good();
}

bool Document::load(const std::wstring& path) {
    std::ifstream input(std::filesystem::path(path), std::ios::binary);
    if (!input) return false;
    std::string magic;
    unsigned int version = 0;
    std::size_t count = 0;
    if (!(input >> magic >> version) || magic != "OGS_PROJECT" || (version < 1 || version > 3) ||
        !(input >> count) || count > 100000) {
        return false;
    }
    std::uint32_t loaded_spawn_id = 0;
    unsigned int loaded_game_type = static_cast<unsigned int>(GameType::Platformer);
    std::uint32_t loaded_background = 0x0B0E16;
    int loaded_par = 3;
    std::string loaded_hint = "Reach the golden goal.";
    if (version >= 2 && (!(input >> loaded_spawn_id >> loaded_game_type >> loaded_background) ||
        loaded_game_type > static_cast<unsigned int>(GameType::SideScroller) || loaded_background > 0xFFFFFFU)) {
        return false;
    }
    if (version >= 3 &&
        (!(input >> loaded_par >> std::quoted(loaded_hint)) || loaded_par < 1 || loaded_par > 9 ||
         loaded_hint.empty() || loaded_hint.size() > 70)) {
        return false;
    }
    std::vector<SceneObject> loaded;
    loaded.reserve(count);
    std::unordered_set<std::uint32_t> ids;
    std::uint32_t max_id = 0;
    for (std::size_t i = 0; i < count; ++i) {
        SceneObject object{};
        unsigned int kind = 0;
        if (!(input >> object.id >> kind >> object.position.x >> object.position.y >> object.scale)) {
            return false;
        }
        if (version >= 3) {
            if (!(input >> object.mass >> object.orbit_rad >> object.orbit_spd >> object.mover_amp >>
                  object.mover_spd >> object.value)) {
                return false;
            }
        }
        if (!(input >> std::quoted(object.name)) ||
            object.id == 0 || !ids.insert(object.id).second ||
            kind > static_cast<unsigned int>(ObjectKind::Gate) ||
            !std::isfinite(object.position.x) || !std::isfinite(object.position.y) || !std::isfinite(object.scale) ||
            object.scale < 0.2F || object.scale > 5.0F || !std::isfinite(object.mass) ||
            !std::isfinite(object.orbit_rad) || !std::isfinite(object.orbit_spd) ||
            !std::isfinite(object.mover_amp) || !std::isfinite(object.mover_spd) || object.value < 0 ||
            object.value > 99 || object.mass < 0.0F || object.mass > 1500.0F || object.orbit_rad < 0.0F ||
            object.orbit_rad > 120.0F || object.orbit_spd < -2.0F || object.orbit_spd > 2.0F ||
            object.mover_amp < 0.0F || object.mover_amp > 120.0F || object.mover_spd < 0.0F ||
            object.mover_spd > 3.0F) {
            return false;
        }
        object.kind = static_cast<ObjectKind>(kind);
        max_id = std::max(max_id, object.id);
        loaded.push_back(std::move(object));
    }
    std::string trailing;
    if (input >> trailing) return false;
    if (version == 1) {
        const auto first_orb = std::find_if(loaded.begin(), loaded.end(), [](const SceneObject& object) {
            return object.kind == ObjectKind::Orb;
        });
        loaded_spawn_id = first_orb == loaded.end() ? 0 : first_orb->id;
    }
    if (max_id == std::numeric_limits<std::uint32_t>::max()) return false;
    if (loaded_spawn_id != 0) {
        const auto spawn = std::find_if(loaded.begin(), loaded.end(), [loaded_spawn_id](const SceneObject& object) {
            return object.id == loaded_spawn_id && object.kind == ObjectKind::Orb;
        });
        if (spawn == loaded.end()) return false;
    }
    checkpoint();
    objects_ = std::move(loaded);
    next_id_ = max_id + 1;
    selected_id_ = 0;
    player_spawn_id_ = loaded_spawn_id;
    game_type_ = static_cast<GameType>(loaded_game_type);
    background_color_ = loaded_background;
    par_ = version >= 3 ? loaded_par : 3;
    hint_ = version >= 3 ? std::move(loaded_hint) : "Reach the golden goal.";
    return true;
}

} // namespace ogs::editor
