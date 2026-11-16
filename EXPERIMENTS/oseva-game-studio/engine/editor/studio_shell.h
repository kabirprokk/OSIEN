// OGS Engine | version 0.1.0 | Professional no-code Studio shell
#pragma once

#if defined(_WIN32)
#include <windows.h>
#include <string>
#include "editor/document.h"

namespace ogs::studio {

struct StudioLayout {
    int width = 0;
    int height = 0;
    int left_width = 0;
    int right_width = 0;
    RECT canvas{};
    float canvas_scale = 1.0F;
};

enum class InspectorField { Name, PositionX, PositionY, Scale };

StudioLayout layout_for(const RECT& client) noexcept;
Vec2 screen_to_world(const StudioLayout& layout, int x, int y) noexcept;
void draw(HDC dc, const RECT& client, const std::wstring& status, const editor::Document& document,
          int hierarchy_scroll = 0, bool show_game_type_chooser = false,
          const std::wstring& project_name = L"UNTITLED PROJECT", bool snap_on = true);
bool click(int x, int y, const RECT& client, std::wstring& status);
bool template_button_hit(int x, int y, const RECT& client) noexcept;
std::uint32_t sidebar_object_hit(int x, int y, const RECT& client,
                                const editor::Document& document, int hierarchy_scroll) noexcept;
bool inspector_field_hit(int x, int y, const RECT& client, InspectorField& field) noexcept;
RECT inspector_field_rect(const RECT& client, InspectorField field) noexcept;
bool game_type_choice_hit(int x, int y, const RECT& client, editor::GameType& type) noexcept;
bool game_type_row_hit(int x, int y, const RECT& client, editor::GameType& type) noexcept;
bool set_spawn_button_hit(int x, int y, const RECT& client) noexcept;
bool object_action_hit(int x, int y, const RECT& client) noexcept;
bool background_button_hit(int x, int y, const RECT& client) noexcept;

} // namespace ogs::studio
#endif

