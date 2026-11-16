// OGS Engine | version 0.1.0 | Core regression tests
#include "core/types.h"
#include "core/fixed_clock.h"
#include "physics/physics_world.h"
#include "editor/document.h"
#include "editor/browser_game.h"
#if defined(_WIN32)
#include "editor/studio_shell.h"
#endif

#include <cassert>
#include <cmath>
#include <filesystem>
#include <limits>

int main() {
    constexpr ogs::Vec2 first{2.0F, 3.0F};
    constexpr ogs::Vec2 second{4.0F, 5.0F};
    constexpr ogs::Vec2 sum = first + second;

    static_assert(sum.x == 6.0F);
    static_assert(sum.y == 8.0F);
    assert(sum.x == 6.0F);
    assert(sum.y == 8.0F);

    constexpr ogs::LogicalSize size{};
    static_assert(size.width == 960);
    static_assert(size.height == 540);
    assert(size.width == 960);
    assert(size.height == 540);

    ogs::FixedClock clock;
    clock.reset(0.0);
    assert(clock.advance(1.0 / 60.0) == 2);
    assert(clock.total_steps() == 2);
    assert(clock.advance(10.0) == ogs::FixedClock::kMaxStepsPerFrame);
    assert(clock.advance(9.0) == 0);

    ogs::physics::PhysicsWorld world;
    const auto body_id = world.create_body({0, {-100.0F, 0.0F}, {0.0F, 0.0F}, 12.0F, 1.0F, 1.0F, true});
    world.add_gravity_well({{0.0F, 0.0F}, 900.0F, 500.0F, false});
    world.step(static_cast<float>(ogs::FixedClock::kStepSeconds));
    const ogs::physics::Body* body = world.body(body_id);
    assert(body != nullptr);
    assert(body->velocity.x > 0.0F);

    const float velocity_before_invalid_step = body->velocity.x;
    world.step(std::numeric_limits<float>::quiet_NaN());
    assert(world.body(body_id)->velocity.x == velocity_before_invalid_step);

    const auto center_body_id = world.create_body({0, {0.001F, 0.0F}, {}, 12.0F, 1.0F, 1.0F, true});
    world.step(static_cast<float>(ogs::FixedClock::kStepSeconds));
    const auto* center_body = world.body(center_body_id);
    assert(center_body != nullptr);
    assert(std::isfinite(center_body->position.x));
    assert(std::isfinite(center_body->velocity.x));

    ogs::editor::Document document;
    const auto object_id = document.add(ogs::editor::ObjectKind::Orb, {12.0F, -8.0F});
    assert(object_id != 0);
    assert(document.objects().size() == 1);
    assert(document.rename(object_id, "Renamed Orb"));
    assert(document.find(object_id)->name == "Renamed Orb");
    assert(!document.rename(object_id, ""));
#if defined(_WIN32)
    const RECT studio_client{0, 0, 1400, 900};
    const auto studio_layout = ogs::studio::layout_for(studio_client);
    assert(ogs::studio::sidebar_object_hit(40, 58 + 122 + 12, studio_client, document, 0) == object_id);
    ogs::studio::InspectorField inspector_field{};
    RECT name_field = ogs::studio::inspector_field_rect(studio_client, ogs::studio::InspectorField::Name);
    assert(ogs::studio::inspector_field_hit((name_field.left + name_field.right) / 2,
        (name_field.top + name_field.bottom) / 2, studio_client, inspector_field));
    assert(inspector_field == ogs::studio::InspectorField::Name);
    for (const auto field : {ogs::studio::InspectorField::PositionX,
                             ogs::studio::InspectorField::PositionY,
                             ogs::studio::InspectorField::Scale}) {
        const RECT field_rect = ogs::studio::inspector_field_rect(studio_client, field);
        assert(ogs::studio::inspector_field_hit((field_rect.left + field_rect.right) / 2,
            (field_rect.top + field_rect.bottom) / 2, studio_client, inspector_field));
        assert(inspector_field == field);
    }
    assert(studio_layout.left_width == 280);
    ogs::editor::Document hierarchy_document;
    for (int index = 0; index < 25; ++index) {
        hierarchy_document.add(ogs::editor::ObjectKind::Platform, {static_cast<float>(index), 10.0F});
    }
    const std::uint32_t scrolled_sidebar_id = ogs::studio::sidebar_object_hit(
        40, 58 + 122 + 12, studio_client, hierarchy_document, 5);
    assert(scrolled_sidebar_id == hierarchy_document.objects()[4].id);
#endif
    document.begin_edit();
    assert(document.move(object_id, {30.0F, 40.0F}));
    assert(document.undo());
    assert(document.find(object_id)->position.x == 12.0F);
    assert(document.redo());
    assert(document.find(object_id)->position.x == 30.0F);
    // Player spawn: first orb auto-becomes spawn; explicit set works; non-orb rejected.
    assert(document.player_spawn_id() == object_id);
    assert(document.is_player_spawn(object_id));
    const auto platform_id = document.add(ogs::editor::ObjectKind::Platform, {0.0F, 0.0F});
    assert(!document.set_player_spawn(platform_id));
    assert(document.set_player_spawn(object_id));
    // Game type + background color contract.
    assert(document.game_type() == ogs::editor::GameType::Platformer);
    assert(document.set_game_type(ogs::editor::GameType::TopDown));
    assert(document.game_type() == ogs::editor::GameType::TopDown);
    assert(!document.set_game_type(static_cast<ogs::editor::GameType>(99)));
    assert(document.background_color() == 0x0B0E16);
    assert(document.set_background_color(0x112233));
    assert(document.background_color() == 0x112233);
    assert(!document.set_background_color(0x1000000U));
    assert(document.undo());  // bg color undo restores previous value
    assert(document.background_color() == 0x0B0E16);
    assert(document.redo());
    assert(document.background_color() == 0x112233);
    document.find(object_id)->name = "</script><script>alert(1)</script>";
    const std::string browser_game = ogs::editor::make_browser_game_html(document);
    assert(browser_game.find("requestAnimationFrame(frame)") != std::string::npos);
    assert(browser_game.find("\\u003c/script\\u003e") != std::string::npos);
    assert(browser_game.find("\"kind\":\"orb\"") != std::string::npos);
    // 1:1 placement regression: fixed 960x540 world, no follow camera.
    assert(browser_game.find("ctx.translate(480,270)") != std::string::npos);
    assert(browser_game.find("cameraX") == std::string::npos);
    assert(browser_game.find("top_down") != std::string::npos);
    assert(browser_game.find("#112233") != std::string::npos);
    // Neon Pulse + Orbit Sling primitives must ship in the runner.
    assert(browser_game.find("neonFlip") != std::string::npos);
    assert(browser_game.find("slingRelease") != std::string::npos);
    assert(browser_game.find("pointerdown") != std::string::npos);
    // New object kinds + physics params + level par/hint round-trip.
    const auto planet_id = document.add(ogs::editor::ObjectKind::Planet, {100.0F, 50.0F});
    assert(document.find(planet_id)->mass == 420.0F);
    assert(document.set_mass(planet_id, 900.0F));
    assert(!document.set_mass(platform_id, 500.0F));
    assert(!document.set_mass(planet_id, std::numeric_limits<float>::quiet_NaN()));
    assert(document.set_orbit(planet_id, 48.0F, 0.6F));
    assert(!document.set_orbit(platform_id, 10.0F, 1.0F));
    assert(document.set_mover(platform_id, 30.0F, 1.2F));
    assert(document.find(platform_id)->mover_amp == 30.0F);
    assert(!document.set_mover(planet_id, 10.0F, 1.0F));
    const auto coin_id = document.add(ogs::editor::ObjectKind::Coin, {-50.0F, 20.0F});
    assert(document.cycle_object_action(coin_id) != nullptr);  // coin -> big coin
    assert(document.find(coin_id)->scale == 2.0F);
    assert(document.cycle_object_action(planet_id) != nullptr);  // heavy -> roaming
    assert(document.find(planet_id)->orbit_rad == 48.0F);
    assert(document.cycle_object_action(999999) == nullptr);
    assert(document.object_action_label(planet_id) != nullptr);
    assert(document.object_action_label(999999) == nullptr);
    assert(document.par() == 3);
    assert(document.set_par(5));
    assert(!document.set_par(0) && !document.set_par(10));
    assert(document.set_hint("Bend it like gravity."));
    assert(!document.set_hint(""));
    assert(!document.set_hint("has\nnewline"));
    const std::string browser_game2 = ogs::editor::make_browser_game_html(document);
    assert(browser_game2.find("\"kind\":\"coin\"") != std::string::npos);
    assert(browser_game2.find("\"mass\":900") != std::string::npos);
    assert(browser_game2.find("Bend it like gravity.") != std::string::npos);
    // Sling walls live in world coords (-467/467), never screen pixels.
    assert(browser_game2.find("p.x<-467") != std::string::npos);
    assert(browser_game2.find("p.x>947") == std::string::npos);
    assert(browser_game2.find("p.x<13") == std::string::npos);
    // Game shell: menu overlay, win overlay, persisted best.
    assert(browser_game2.find("CLICK OR TAP TO PLAY") != std::string::npos);
    assert(browser_game2.find("oseva-preview-best") != std::string::npos);
    assert(browser_game2.find("Press R to play again") != std::string::npos);
#if defined(_WIN32)
    // Studio hit areas for spawn / background / game-type must resolve.
    {
        const RECT hit_client{0, 0, 1400, 900};
        ogs::editor::GameType picked{};
        RECT spawn_rect{0, 0, 0, 0};
        // Compute expected spawn rect center from layout: right panel, y 58+454..484.
        const auto hit_layout = ogs::studio::layout_for(hit_client);
        const int sx = hit_layout.width - hit_layout.right_width + 40;
        const int sy = 58 + 464;
        assert(ogs::studio::set_spawn_button_hit(sx, sy, hit_client));
        assert(ogs::studio::object_action_hit(sx, 58 + 515, hit_client));
        assert(ogs::studio::background_button_hit(sx, 58 + 601, hit_client));
        assert(ogs::studio::game_type_row_hit(sx, 58 + 309, hit_client, picked));
        assert(picked == ogs::editor::GameType::Platformer);
        // Ghost-click guard: canvas center (invisible chooser area) must NOT
        // switch the game type once the chooser is closed.
        assert(!ogs::studio::game_type_row_hit(700, 450, hit_client, picked));
        (void)spawn_rect;
    }
#endif
    assert(document.set_scale(object_id, 99.0F));
    assert(document.find(object_id)->scale == 5.0F);
    assert(document.duplicate(object_id) != 0);
    assert(document.objects().size() == 5);

    const std::filesystem::path project_path = std::filesystem::temp_directory_path() / "oseva_document_roundtrip.ogsproject";
    assert(document.save(project_path.wstring()));
    ogs::editor::Document reopened;
    assert(reopened.load(project_path.wstring()));
    assert(reopened.objects().size() == 5);
    assert(reopened.find(object_id) != nullptr);
    assert(reopened.find(object_id)->position.x == 30.0F);
    assert(reopened.player_spawn_id() == object_id);
    assert(reopened.game_type() == ogs::editor::GameType::TopDown);
    assert(reopened.background_color() == 0x112233);
    assert(reopened.find(planet_id) != nullptr);
    assert(reopened.find(planet_id)->mass == 900.0F);
    assert(reopened.find(planet_id)->orbit_rad == 48.0F);
    assert(reopened.find(platform_id)->mover_amp == 30.0F);
    assert(reopened.par() == 5);
    assert(reopened.hint() == "Bend it like gravity.");

    ogs::editor::Document preserved;
    const auto preserved_id = preserved.add(ogs::editor::ObjectKind::Goal, {1.0F, 2.0F});
    assert(!preserved.load((project_path.parent_path() / "missing_oseva_project.ogsproject").wstring()));
    assert(preserved.find(preserved_id) != nullptr);
    std::filesystem::remove(project_path);
    return 0;
}

