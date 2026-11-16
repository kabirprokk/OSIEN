// OGS Engine | version 0.1.0 | Professional no-code Studio shell
#include "editor/studio_shell.h"

#if defined(_WIN32)

#include <algorithm>
#include <string_view>

namespace ogs::studio {
namespace {

constexpr COLORREF kPanel = RGB(20, 20, 22);
constexpr COLORREF kPanelRaised = RGB(29, 29, 32);
constexpr COLORREF kLine = RGB(54, 54, 58);
constexpr COLORREF kInk = RGB(242, 242, 238);
constexpr COLORREF kDim = RGB(169, 169, 165);
constexpr COLORREF kFaint = RGB(105, 105, 103);
constexpr COLORREF kFocus = RGB(224, 224, 219);

void fill(HDC dc, RECT rect, COLORREF color) {
    const HBRUSH brush = CreateSolidBrush(color);
    FillRect(dc, &rect, brush);
    DeleteObject(brush);
}

void line(HDC dc, int x1, int y1, int x2, int y2, COLORREF color) {
    const HPEN pen = CreatePen(PS_SOLID, 1, color);
    const HGDIOBJ old = SelectObject(dc, pen);
    MoveToEx(dc, x1, y1, nullptr);
    LineTo(dc, x2, y2);
    SelectObject(dc, old);
    DeleteObject(pen);
}

void text(HDC dc, const wchar_t* value, RECT rect, COLORREF color, int size, bool bold = false) {
    const HFONT font = CreateFontW(
        -size, 0, 0, 0, bold ? FW_SEMIBOLD : FW_NORMAL, FALSE, FALSE, FALSE,
        DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
        DEFAULT_PITCH | FF_DONTCARE, L"Segoe UI");
    const HGDIOBJ old = SelectObject(dc, font);
    SetBkMode(dc, TRANSPARENT);
    SetTextColor(dc, color);
    DrawTextW(dc, value, -1, &rect, DT_LEFT | DT_VCENTER | DT_SINGLELINE | DT_NOPREFIX);
    SelectObject(dc, old);
    DeleteObject(font);
}

void centered(HDC dc, const wchar_t* value, RECT rect, COLORREF color, int size, bool bold = false) {
    const HFONT font = CreateFontW(
        -size, 0, 0, 0, bold ? FW_SEMIBOLD : FW_NORMAL, FALSE, FALSE, FALSE,
        DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
        DEFAULT_PITCH | FF_DONTCARE, L"Segoe UI");
    const HGDIOBJ old = SelectObject(dc, font);
    SetBkMode(dc, TRANSPARENT);
    SetTextColor(dc, color);
    DrawTextW(dc, value, -1, &rect, DT_CENTER | DT_VCENTER | DT_SINGLELINE | DT_NOPREFIX);
    SelectObject(dc, old);
    DeleteObject(font);
}

void panel(HDC dc, RECT rect, COLORREF color = kPanel) {
    fill(dc, rect, color);
    line(dc, rect.left, rect.top, rect.right, rect.top, kLine);
}

void button(HDC dc, RECT rect, const wchar_t* label, bool primary = false) {
    const COLORREF color = primary ? kFocus : kPanelRaised;
    const COLORREF ink = primary ? RGB(18, 18, 19) : kInk;
    const HBRUSH brush = CreateSolidBrush(color);
    const HPEN pen = CreatePen(PS_SOLID, 1, primary ? kFocus : kLine);
    const HGDIOBJ old_brush = SelectObject(dc, brush);
    const HGDIOBJ old_pen = SelectObject(dc, pen);
    RoundRect(dc, rect.left, rect.top, rect.right, rect.bottom, 8, 8);
    SelectObject(dc, old_brush);
    SelectObject(dc, old_pen);
    DeleteObject(brush);
    DeleteObject(pen);
    centered(dc, label, rect, ink, 12, true);
}

std::wstring utf8_to_wide(const std::string_view value) {
    if (value.empty()) return {};
    const int needed = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, value.data(),
                                            static_cast<int>(value.size()), nullptr, 0);
    if (needed <= 0) return std::wstring(value.begin(), value.end());
    std::wstring result(static_cast<std::size_t>(needed), L'\0');
    MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, value.data(),
                        static_cast<int>(value.size()), result.data(), needed);
    return result;
}

const wchar_t* kind_label(const editor::ObjectKind kind) {
    switch (kind) {
        case editor::ObjectKind::Orb: return L"ORB";
        case editor::ObjectKind::Platform: return L"SLAB";
        case editor::ObjectKind::Planet: return L"WELL";
        case editor::ObjectKind::Hazard: return L"HAZARD";
        case editor::ObjectKind::Goal: return L"GOAL";
        case editor::ObjectKind::Coin: return L"COIN";
        case editor::ObjectKind::Mote: return L"MOTE";
        case editor::ObjectKind::Pad: return L"PAD";
        case editor::ObjectKind::Gate: return L"GATE";
    }
    return L"OBJECT";
}

COLORREF kind_color(const editor::ObjectKind kind) noexcept {
    switch (kind) {
        case editor::ObjectKind::Orb: return RGB(0, 220, 238);
        case editor::ObjectKind::Platform: return RGB(0, 185, 200);
        case editor::ObjectKind::Planet: return RGB(123, 47, 255);
        case editor::ObjectKind::Hazard: return RGB(255, 43, 43);
        case editor::ObjectKind::Goal: return RGB(255, 217, 100);
        case editor::ObjectKind::Coin: return RGB(255, 217, 100);
        case editor::ObjectKind::Mote: return RGB(0, 240, 255);
        case editor::ObjectKind::Pad: return RGB(0, 240, 255);
        case editor::ObjectKind::Gate: return RGB(255, 217, 100);
    }
    return RGB(0, 220, 238);
}

void editable_field(HDC dc, const RECT rect, const wchar_t* value) {
    fill(dc, rect, RGB(15, 15, 18));
    line(dc, rect.left, rect.top, rect.right, rect.top, kLine);
    line(dc, rect.left, rect.bottom - 1, rect.right, rect.bottom - 1, kLine);
    line(dc, rect.left, rect.top, rect.left, rect.bottom, kLine);
    line(dc, rect.right - 1, rect.top, rect.right - 1, rect.bottom, kLine);
    text(dc, value, {rect.left + 8, rect.top + 1, rect.right - 8, rect.bottom - 1}, kInk, 12);
}

const wchar_t* game_type_label(const editor::GameType type) {
    switch (type) {
        case editor::GameType::Platformer: return L"PLATFORMER";
        case editor::GameType::TopDown: return L"TOP-DOWN";
        case editor::GameType::OrbitPuzzle: return L"ORBIT PUZZLE";
        case editor::GameType::SideScroller: return L"SIDE-SCROLLER";
    }
    return L"PLATFORMER";
}

RECT game_type_row_rect(const RECT& client, int index) noexcept {
    const StudioLayout layout = layout_for(client);
    const int left = layout.width - layout.right_width + 18;
    const int right = layout.width - 18;
    const int top = 58 + 296 + index * 32;
    return {left, top, right, top + 26};
}

RECT set_spawn_rect(const RECT& client) noexcept {
    const StudioLayout layout = layout_for(client);
    const int left = layout.width - layout.right_width + 18;
    const int right = layout.width - 18;
    return {left, 58 + 450, right, 58 + 478};
}

RECT object_action_rect(const RECT& client) noexcept {
    const StudioLayout layout = layout_for(client);
    const int left = layout.width - layout.right_width + 18;
    const int right = layout.width - 18;
    return {left, 58 + 502, right, 58 + 528};
}

RECT background_rect(const RECT& client) noexcept {
    const StudioLayout layout = layout_for(client);
    const int left = layout.width - layout.right_width + 18;
    const int right = layout.width - 18;
    return {left, 58 + 588, right, 58 + 614};
}

RECT chooser_card_rect(const RECT& client) noexcept {
    const StudioLayout layout = layout_for(client);
    const int cx = layout.width / 2;
    const int cy = layout.height / 2;
    return {cx - 270, cy - 210, cx + 270, cy + 210};
}

RECT chooser_option_rect(const RECT& client, int index) noexcept {
    const RECT card = chooser_card_rect(client);
    const int left = card.left + 24;
    const int right = card.right - 24;
    const int top = card.top + 70 + index * 80;
    return {left, top, right, top + 72};
}

} // namespace

StudioLayout layout_for(const RECT& client) noexcept {
    StudioLayout result{};
    result.width = client.right - client.left;
    result.height = client.bottom - client.top;
    result.left_width = std::clamp(result.width / 5, 210, 300);
    result.right_width = std::clamp(result.width / 4, 250, 340);
    result.canvas = {result.left_width + 24, 58 + 56,
                     result.width - result.right_width - 24, result.height - 28 - 48};
    const int cw = static_cast<int>(std::max(1L, result.canvas.right - result.canvas.left));
    const int ch = static_cast<int>(std::max(1L, result.canvas.bottom - result.canvas.top));
    result.canvas_scale = std::min(cw / 960.0F, ch / 540.0F);
    return result;
}

RECT inspector_field_rect(const RECT& client, const InspectorField field) noexcept {
    const StudioLayout layout = layout_for(client);
    const int left = layout.width - layout.right_width + 18;
    const int right = layout.width - 18;
    const int middle = (left + right) / 2;
    switch (field) {
        case InspectorField::Name: return {left, 58 + 84, right, 58 + 112};
        case InspectorField::PositionX: return {left, 58 + 172, middle - 6, 58 + 200};
        case InspectorField::PositionY: return {middle + 6, 58 + 172, right, 58 + 200};
        case InspectorField::Scale: return {left, 58 + 232, right, 58 + 260};
    }
    return {};
}

bool inspector_field_hit(const int x, const int y, const RECT& client, InspectorField& field) noexcept {
    constexpr InspectorField fields[] = {InspectorField::Name, InspectorField::PositionX,
        InspectorField::PositionY, InspectorField::Scale};
    for (const InspectorField candidate : fields) {
        RECT rect = inspector_field_rect(client, candidate);
        if (PtInRect(&rect, POINT{x, y})) {
            field = candidate;
            return true;
        }
    }
    return false;
}

std::uint32_t sidebar_object_hit(const int x, const int y, const RECT& client,
                                const editor::Document& document, int hierarchy_scroll) noexcept {
    const StudioLayout layout = layout_for(client);
    constexpr int row_height = 28;
    const int top = 58 + 122;
    const int bottom = layout.height - 28 - 82;
    if (x < 18 || x >= layout.left_width - 18 || y < top || y >= bottom) return 0;
    const int visible = std::max(1, (bottom - top) / row_height);
    const int max_scroll = std::max(0, static_cast<int>(document.objects().size()) - visible);
    hierarchy_scroll = std::clamp(hierarchy_scroll, 0, max_scroll);
    const int index = hierarchy_scroll + (y - top) / row_height;
    if (index < 0 || index >= static_cast<int>(document.objects().size())) return 0;
    return document.objects()[static_cast<std::size_t>(index)].id;
}

Vec2 screen_to_world(const StudioLayout& layout, const int x, const int y) noexcept {
    const float center_x = (layout.canvas.left + layout.canvas.right) * 0.5F;
    const float center_y = (layout.canvas.top + layout.canvas.bottom) * 0.5F;
    const float scale = std::max(0.01F, layout.canvas_scale);
    return {(x - center_x) / scale, (center_y - y) / scale};
}

void draw(HDC dc, const RECT& client, const std::wstring& status, const editor::Document& document,
          int hierarchy_scroll, bool show_game_type_chooser,
          const std::wstring& project_name, bool snap_on) {
    const StudioLayout layout = layout_for(client);
    const int width = layout.width;
    const int height = layout.height;
    const int topbar = 58;
    const int footer = 28;
    const int left_width = layout.left_width;
    const int right_width = layout.right_width;
    const COLORREF kBg = RGB(10, 10, 11);
    fill(dc, client, kBg);

    RECT top{0, 0, width, topbar};
    panel(dc, top, RGB(16, 16, 18));
    text(dc, L"OSEVA", {22, 8, 96, 30}, kInk, 15, true);
    text(dc, L"GAME STUDIO", {22, 28, 150, 48}, kDim, 10, false);
    line(dc, 174, 16, 174, 42, kLine);
    text(dc, project_name.c_str(), {194, 14, 430, 42}, kInk, 13, true);
    wchar_t type_line[64]{};
    swprintf_s(type_line, L"2D GAME \u00B7 %ls", game_type_label(document.game_type()));
    text(dc, type_line, {194, 33, 420, 52}, kFaint, 10);
    button(dc, {width - 458, 14, width - 384, 44}, L"UNDO");
    button(dc, {width - 376, 14, width - 302, 44}, L"REDO");
    button(dc, {width - 294, 14, width - 208, 44}, L"SAVE");
    button(dc, {width - 198, 14, width - 20, 44}, L"\u25B6  RUN", true);

    const int body_top = topbar;
    const int body_bottom = height - footer;
    RECT left{0, body_top, left_width, body_bottom};
    RECT center{left_width, body_top, width - right_width, body_bottom};
    RECT right{width - right_width, body_top, width, body_bottom};
    panel(dc, left);
    panel(dc, right);
    fill(dc, center, RGB(13, 13, 15));

    text(dc, L"SCENES", {18, body_top + 18, left_width - 18, body_top + 40}, kDim, 10, true);
    RECT scene{18, body_top + 48, left_width - 18, body_top + 80};
    fill(dc, scene, RGB(43, 38, 54));
    text(dc, L"Main Scene", {30, body_top + 48, left_width - 28, body_top + 80}, kInk, 12, true);
    wchar_t hierarchy_title[48]{};
    swprintf_s(hierarchy_title, L"HIERARCHY  (%zu)", document.objects().size());
    text(dc, hierarchy_title, {18, body_top + 94, left_width - 18, body_top + 116}, kDim, 10, true);
    const int hierarchy_top = body_top + 122;
    const int hierarchy_bottom = body_bottom - 82;
    constexpr int hierarchy_row_height = 28;
    const int hierarchy_visible = std::max(1, (hierarchy_bottom - hierarchy_top) / hierarchy_row_height);
    const int max_scroll = std::max(0, static_cast<int>(document.objects().size()) - hierarchy_visible);
    hierarchy_scroll = std::clamp(hierarchy_scroll, 0, max_scroll);
    const int hierarchy_end = std::min(static_cast<int>(document.objects().size()), hierarchy_scroll + hierarchy_visible);
    for (int index = hierarchy_scroll; index < hierarchy_end; ++index) {
        const editor::SceneObject& object = document.objects()[static_cast<std::size_t>(index)];
        const int row_top = hierarchy_top + (index - hierarchy_scroll) * hierarchy_row_height;
        RECT row{18, row_top, left_width - 18, row_top + hierarchy_row_height - 2};
        if (object.id == document.selected_id()) fill(dc, row, RGB(43, 38, 54));
        const COLORREF icon_color = kind_color(object.kind);
        RECT icon{row.left + 7, row.top + 8, row.left + 15, row.top + 16};
        fill(dc, icon, icon_color);
        std::wstring label = utf8_to_wide(object.name);
        if (document.is_player_spawn(object.id)) label += L"  \u2605 SPAWN";
        text(dc, label.c_str(), {row.left + 22, row.top, row.right - 8, row.top + 17}, kInk, 11,
             object.id == document.selected_id());
        text(dc, kind_label(object.kind), {row.left + 22, row.top + 14, row.right - 8, row.bottom}, kFaint, 8);
    }
    if (document.objects().empty()) {
        text(dc, L"No objects yet", {24, hierarchy_top + 6, left_width - 18, hierarchy_top + 30}, kFaint, 11);
        text(dc, L"Add one to this scene", {24, hierarchy_top + 28, left_width - 18, hierarchy_top + 52}, kFaint, 10);
    }
    line(dc, 18, body_bottom - 74, left_width - 18, body_bottom - 74, kLine);
    button(dc, {18, body_bottom - 58, left_width - 18, body_bottom - 22}, L"+  ADD OBJECT");

    text(dc, L"SCENE / MAIN  \u00B7  960 x 540 WORLD (0,0 = CENTER)", {left_width + 24, body_top + 18, width - right_width - 24, body_top + 42}, kDim, 10, true);
    const int canvas_left = left_width + 24;
    const int canvas_top = body_top + 56;
    const int canvas_right = width - right_width - 24;
    const int canvas_bottom = body_bottom - 48;
    RECT canvas{canvas_left, canvas_top, canvas_right, canvas_bottom};
    const std::uint32_t bg = document.background_color();
    fill(dc, canvas, RGB(GetRValue(0) * 0 + ((bg >> 16) & 0xFF), ((bg >> 8) & 0xFF), (bg & 0xFF)));
    line(dc, canvas.left, canvas.top, canvas.right, canvas.top, kLine);
    line(dc, canvas.left, canvas.bottom, canvas.right, canvas.bottom, kLine);
    for (int x = canvas.left + 20; x < canvas.right; x += 40) {
        line(dc, x, canvas.top, x, canvas.bottom, RGB(20, 23, 30));
    }
    for (int y = canvas.top + 20; y < canvas.bottom; y += 40) {
        line(dc, canvas.left, y, canvas.right, y, RGB(20, 23, 30));
    }
    // Center cross: world origin marker so editor matches the browser runner 1:1.
    {
        const int cx = (canvas.left + canvas.right) / 2;
        const int cy = (canvas.top + canvas.bottom) / 2;
        line(dc, cx - 10, cy, cx + 10, cy, RGB(90, 90, 96));
        line(dc, cx, cy - 10, cx, cy + 10, RGB(90, 90, 96));
    }
    const float object_scale = layout.canvas_scale;
    const float object_center_x = (canvas.left + canvas.right) * 0.5F;
    const float object_center_y = (canvas.top + canvas.bottom) * 0.5F;
    for (const editor::SceneObject& object : document.objects()) {
        const int ox = static_cast<int>(object_center_x + object.position.x * object_scale);
        const int oy = static_cast<int>(object_center_y - object.position.y * object_scale);
        const int radius = std::max(7, static_cast<int>(18.0F * object.scale * object_scale));
        const COLORREF color = kind_color(object.kind);
        // Spawn marker: gold outer ring for the saved player spawn.
        if (document.is_player_spawn(object.id)) {
            const HBRUSH spawn_brush = CreateSolidBrush(RGB(10, 12, 19));
            const HPEN spawn_pen = CreatePen(PS_SOLID, 3, RGB(255, 217, 100));
            const HGDIOBJ old_b = SelectObject(dc, spawn_brush);
            const HGDIOBJ old_p = SelectObject(dc, spawn_pen);
            Ellipse(dc, ox - radius - 6, oy - radius - 6, ox + radius + 6, oy + radius + 6);
            SelectObject(dc, old_b);
            SelectObject(dc, old_p);
            DeleteObject(spawn_brush);
            DeleteObject(spawn_pen);
        }
        const HBRUSH object_brush = CreateSolidBrush(RGB(10, 12, 19));
        const HPEN object_pen = CreatePen(PS_SOLID, object.id == document.selected_id() ? 2 : 1, color);
        const HGDIOBJ old_object_brush = SelectObject(dc, object_brush);
        const HGDIOBJ old_object_pen = SelectObject(dc, object_pen);
        if (object.kind == editor::ObjectKind::Platform) {
            RoundRect(dc, ox - radius * 2, oy - radius / 2, ox + radius * 2, oy + radius / 2, 6, 6);
        } else if (object.kind == editor::ObjectKind::Hazard) {
            POINT hazard[] = {{ox, oy - radius}, {ox + radius, oy + radius}, {ox - radius, oy + radius}};
            Polygon(dc, hazard, 3);
        } else if (object.kind == editor::ObjectKind::Coin) {
            Ellipse(dc, ox - radius / 2, oy - radius / 3, ox + radius / 2, oy + radius / 3);
            Ellipse(dc, ox - radius / 4, oy - radius / 6, ox + radius / 4, oy + radius / 6);
        } else if (object.kind == editor::ObjectKind::Mote) {
            const int dot = std::max(3, radius / 3);
            Ellipse(dc, ox - dot, oy - dot, ox + dot, oy + dot);
        } else if (object.kind == editor::ObjectKind::Pad) {
            Ellipse(dc, ox - radius, oy - radius, ox + radius, oy + radius);
            const int dot = std::max(3, radius / 4);
            Ellipse(dc, ox - dot, oy - dot, ox + dot, oy + dot);
        } else if (object.kind == editor::ObjectKind::Gate) {
            RoundRect(dc, ox - radius / 2, oy - radius * 2, ox + radius / 2, oy + radius * 2, 4, 4);
            line(dc, ox - radius / 2, oy, ox + radius / 2, oy, color);
        } else {
            Ellipse(dc, ox - radius, oy - radius, ox + radius, oy + radius);
            if (object.kind == editor::ObjectKind::Goal) {
                Ellipse(dc, ox - radius / 2, oy - radius / 2, ox + radius / 2, oy + radius / 2);
            }
        }
        SelectObject(dc, old_object_brush);
        SelectObject(dc, old_object_pen);
        DeleteObject(object_brush);
        DeleteObject(object_pen);
        RECT name_rect{ox + radius + 5, oy - 10, ox + radius + 165, oy + 12};
        std::wstring object_name = utf8_to_wide(object.name);
        if (document.is_player_spawn(object.id)) object_name += L" \u2605";
        text(dc, object_name.c_str(), name_rect, kDim, 10);
    }
    const int content_height = canvas.bottom - canvas.top;
    const int content_center = canvas.top + content_height / 2;
    if (document.objects().empty() && !show_game_type_chooser) {
        centered(dc, L"START BUILDING", {canvas.left, content_center - 88, canvas.right, content_center - 52}, kInk, 22, true);
        centered(dc, L"Choose an object from the library or start with a template.",
                 {canvas.left + 16, content_center - 44, canvas.right - 16, content_center - 16}, kDim, 12);
        const int buttons_width = 264;
        const int buttons_left = (canvas.left + canvas.right - buttons_width) / 2;
        button(dc, {buttons_left, content_center + 18, buttons_left + 124, content_center + 62}, L"NEW LEVEL", true);
        button(dc, {buttons_left + 140, content_center + 18, buttons_left + buttons_width, content_center + 62}, L"TEMPLATES");
    }
    wchar_t snap_line[64]{};
    swprintf_s(snap_line, L"960 \u00D7 540   \u2022   %ls   \u2022   1:1 WITH BROWSER RUN", snap_on ? L"SNAP 8" : L"FREE PLACE");
    text(dc, snap_line, {canvas.left, body_bottom - 34, canvas.right, body_bottom - 10}, kFaint, 10);

    text(dc, L"INSPECTOR", {width - right_width + 18, body_top + 18, width - 18, body_top + 42}, kDim, 10, true);
    line(dc, width - right_width + 18, body_top + 52, width - 18, body_top + 52, kLine);
    const editor::SceneObject* selected = document.find(document.selected_id());
    if (selected == nullptr) {
        centered(dc, L"Nothing selected", {width - right_width + 18, body_top + 130, width - 18, body_top + 160}, kInk, 14, true);
        centered(dc, L"Select an object to edit its properties.",
                 {width - right_width + 32, body_top + 166, width - 32, body_top + 212}, kFaint, 11);
    } else {
        const std::wstring object_title = utf8_to_wide(selected->name);
        text(dc, L"NAME  (click to edit)", {width - right_width + 18, body_top + 62, width - 18, body_top + 82}, kDim, 9, true);
        editable_field(dc, inspector_field_rect(client, InspectorField::Name), object_title.c_str());
        text(dc, L"POSITION  (world 0,0 = center)", {width - right_width + 18, body_top + 126, width - 18, body_top + 150}, kDim, 9, true);
        wchar_t position_x[48]{};
        wchar_t position_y[48]{};
        swprintf_s(position_x, L"%.2f", selected->position.x);
        swprintf_s(position_y, L"%.2f", selected->position.y);
        text(dc, L"X", {width - right_width + 18, body_top + 150, width - right_width + 100, body_top + 170}, kDim, 9, true);
        text(dc, L"Y", {width - right_width + 136, body_top + 150, width - 18, body_top + 170}, kDim, 9, true);
        editable_field(dc, inspector_field_rect(client, InspectorField::PositionX), position_x);
        editable_field(dc, inspector_field_rect(client, InspectorField::PositionY), position_y);
        text(dc, L"SCALE  (0.2 \u2014 5.0)", {width - right_width + 18, body_top + 210, width - 18, body_top + 230}, kDim, 9, true);
        wchar_t scale[48]{};
        swprintf_s(scale, L"%.2f", selected->scale);
        editable_field(dc, inspector_field_rect(client, InspectorField::Scale), scale);
    }
    // Game type section: click a row to switch. Top-down included.
    text(dc, L"GAME TYPE  (click to switch)", {width - right_width + 18, body_top + 272, width - 18, body_top + 292}, kDim, 9, true);
    {
        constexpr editor::GameType kTypes[] = {editor::GameType::Platformer, editor::GameType::TopDown,
            editor::GameType::OrbitPuzzle, editor::GameType::SideScroller};
        constexpr const wchar_t* kHints[] = {L"Run + jump  (WASD/arrows)", L"Move 8-way  (WASD/arrows)",
            L"Orbit physics + jump", L"Auto-run style + jump"};
        for (int i = 0; i < 4; ++i) {
            const RECT row = game_type_row_rect(client, i);
            const bool active = document.game_type() == kTypes[i];
            button(dc, row, game_type_label(kTypes[i]), active);
            RECT hint{row.right - 168, row.top, row.right - 8, row.bottom};
            text(dc, kHints[i], hint, active ? RGB(18, 18, 19) : kFaint, 9);
        }
    }
    text(dc, L"PLAYER SPAWN", {width - right_width + 18, body_top + 428, width - 18, body_top + 446}, kDim, 9, true);
    {
        const RECT spawn_btn = set_spawn_rect(client);
        const bool can_spawn = selected != nullptr && selected->kind == editor::ObjectKind::Orb;
        button(dc, spawn_btn, document.is_player_spawn(document.selected_id()) ? L"\u2605  SPAWN SET" : L"SET SPAWN HERE", document.is_player_spawn(document.selected_id()));
        if (!can_spawn && document.selected_id() != 0) {
            text(dc, L"Select an ORB to set spawn.", {spawn_btn.left, spawn_btn.bottom + 2, spawn_btn.right, spawn_btn.bottom + 18}, kFaint, 9);
        } else if (document.player_spawn_id() == 0) {
            text(dc, L"No spawn yet: add an ORB.", {spawn_btn.left, spawn_btn.bottom + 2, spawn_btn.right, spawn_btn.bottom + 18}, kFaint, 9);
        }
    }
    wchar_t action_title[96]{};
    wcscpy_s(action_title, L"OBJECT ACTION");
    {
        const editor::SceneObject* probe = document.find(document.selected_id());
        if (probe != nullptr && probe->kind == editor::ObjectKind::Planet) {
            swprintf_s(action_title, L"WELL \u00B7 PULL %.0f \u00B7 %ls", probe->mass,
                       probe->orbit_rad > 0.0F ? L"ROAMING" : L"STATIC");
        } else if (probe != nullptr && probe->kind == editor::ObjectKind::Platform) {
            if (probe->mover_amp > 0.0F)
                swprintf_s(action_title, L"SLAB \u00B7 MOVER %.0fpx @ %.1f", probe->mover_amp, probe->mover_spd);
            else
                wcscpy_s(action_title, L"SLAB \u00B7 STATIC");
        } else if (probe != nullptr && probe->kind == editor::ObjectKind::Coin) {
            swprintf_s(action_title, L"COIN \u00B7 +%d", probe->value > 0 ? probe->value : (probe->scale > 1.5F ? 5 : 1));
        }
    }
    text(dc, action_title, {width - right_width + 18, body_top + 482, width - 18, body_top + 500}, kDim, 9, true);
    {
        const RECT action_btn = object_action_rect(client);
        const editor::SceneObject* action_object = document.find(document.selected_id());
        const char* action_label = action_object != nullptr ? document.object_action_label(action_object->id) : nullptr;
        if (action_label == nullptr) {
            text(dc, L"Select a well, slab or coin.", {action_btn.left, action_btn.top + 4, action_btn.right, action_btn.bottom}, kFaint, 10);
        } else {
            const std::wstring wide_label = utf8_to_wide(action_label);
            std::wstring caption = wide_label + L"  \u25B6";
            button(dc, action_btn, caption.c_str(), false);
        }
    }
    wchar_t level_line[128]{};
    swprintf_s(level_line, L"LEVEL  \u00B7  PAR %d", document.par());
    text(dc, level_line, {width - right_width + 18, body_top + 532, width - 18, body_top + 550}, kDim, 9, true);
    {
        const std::wstring hint = utf8_to_wide(document.hint());
        text(dc, (L"\u201C" + hint + L"\u201D").c_str(), {width - right_width + 18, body_top + 550, width - 18, body_top + 568}, kFaint, 9);
    }
    text(dc, L"BACKGROUND", {width - right_width + 18, body_top + 570, width - 18, body_top + 586}, kDim, 9, true);
    {
        const RECT bg_btn = background_rect(client);
        const std::uint32_t rgb = document.background_color();
        const HBRUSH swatch = CreateSolidBrush(RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
        const HGDIOBJ old = SelectObject(dc, swatch);
        RoundRect(dc, bg_btn.left, bg_btn.top, bg_btn.left + 44, bg_btn.bottom, 6, 6);
        SelectObject(dc, old);
        DeleteObject(swatch);
        wchar_t hex[16]{};
        swprintf_s(hex, L"#%06X", rgb & 0xFFFFFFU);
        text(dc, hex, {bg_btn.left + 50, bg_btn.top, bg_btn.left + 120, bg_btn.bottom}, kInk, 11, true);
        button(dc, {bg_btn.right - 110, bg_btn.top, bg_btn.right, bg_btn.bottom}, L"CHANGE");
    }
    line(dc, width - right_width + 18, body_top + 624, width - 18, body_top + 624, kLine);
    text(dc, L"CONTROLS", {width - right_width + 18, body_top + 638, width - 18, body_top + 658}, kDim, 10, true);
    text(dc, L"Neon: tap/Space flips · Sling: drag orb.", {width - right_width + 18, body_top + 664, width - 18, body_top + 684}, kInk, 11);
    text(dc, L"L level (par+hint) · Del remove · F5 run.", {width - right_width + 18, body_top + 686, width - 18, body_top + 706}, kDim, 11);
    text(dc, L"Ctrl+N game type · Ctrl+S save.", {width - right_width + 18, body_top + 708, width - 18, body_top + 728}, kDim, 11);
    text(dc, L"G toggles snap-8 / free place.", {width - right_width + 18, body_top + 730, width - 18, body_top + 750}, kDim, 11);

    RECT bottom{0, height - footer, width, height};
    fill(dc, bottom, RGB(17, 17, 19));
    text(dc, L"\u25CF  READY", {18, height - footer, 120, height}, RGB(137, 205, 154), 10, true);
    text(dc, status.c_str(), {132, height - footer, width - 260, height}, kDim, 10);
    text(dc, L"WINDOWS  \u2022  OGS ENGINE 0.1", {width - 210, height - footer, width - 18, height}, kFaint, 10);

    if (show_game_type_chooser) {
        // Dim everything behind the chooser.
        {
            const HBRUSH dim = CreateSolidBrush(RGB(4, 4, 6));
            // Simulate translucency with a dark fill; chooser card stays bright.
            RECT full{0, 0, width, height};
            // Draw a simple dark overlay by filling with a checker of lines? Keep solid for clarity.
            FillRect(dc, &full, dim);
            DeleteObject(dim);
        }
        // Redraw a clean backdrop so the chooser is readable.
        fill(dc, client, RGB(10, 10, 11));
        const RECT card = chooser_card_rect(client);
        fill(dc, card, RGB(22, 22, 25));
        line(dc, card.left, card.top, card.right, card.top, kLine);
        line(dc, card.left, card.bottom - 1, card.right, card.bottom - 1, kLine);
        centered(dc, L"WHAT DO YOU WANT TO MAKE?", {card.left, card.top + 14, card.right, card.top + 40}, kDim, 11, true);
        centered(dc, L"OSEVA GAME STUDIO", {card.left, card.top + 36, card.right, card.top + 58}, kInk, 18, true);
        constexpr const wchar_t* kTitles[] = {L"PLATFORMER", L"TOP-DOWN ADVENTURE", L"ORBIT PUZZLE", L"SIDE-SCROLLER"};
        constexpr const wchar_t* kDescs[] = {
            L"Run, jump, dodge hazards, reach the goal.  WASD / arrows.",
            L"Move in all 8 directions. No gravity.  Perfect for RPGs + mazes.",
            L"Sling + gravity wells pull you.  Planets bend your path.",
            L"Keep moving, time your jumps.  Fast arcade levels."};
        for (int i = 0; i < 4; ++i) {
            const RECT opt = chooser_option_rect(client, i);
            button(dc, opt, kTitles[i], true);
            RECT desc{opt.left + 14, opt.top + 30, opt.right - 14, opt.bottom - 6};
            text(dc, kDescs[i], desc, RGB(18, 18, 19), 11);
        }
        centered(dc, L"Click a type to start. You can switch anytime in the Inspector.",
                 {card.left, card.bottom - 26, card.right, card.bottom - 6}, kFaint, 10);
    }
}

bool click(const int x, const int y, const RECT& client, std::wstring& status) {
    const StudioLayout layout = layout_for(client);
    const int width = layout.width;
    const int height = layout.height;
    const int left_width = layout.left_width;
    const int right_width = layout.right_width;
    const int topbar = 58;
    const int footer = 28;
    if (y < 58 && x >= width - 294 && x < width - 208) {
        status = L"Project saved.";
        return false;
    } else if (x < left_width && y > height - footer - 86) {
        status = L"Choose an object type to add to the scene.";
        return false;
    } else if (x >= left_width && x < width - right_width) {
        const int canvas_left = left_width + 24;
        const int canvas_right = width - right_width - 24;
        const int canvas_top = topbar + 56;
        const int canvas_bottom = height - footer - 48;
        const int center_y = canvas_top + (canvas_bottom - canvas_top) / 2;
        const int button_left = (canvas_left + canvas_right - 264) / 2;
        if (x >= button_left && x < button_left + 124 && y >= center_y + 18 && y < center_y + 62) {
            status = L"Live scene preview running.";
            return true;
        }
    }
    return false;
}

bool template_button_hit(const int x, const int y, const RECT& client) noexcept {
    const StudioLayout layout = layout_for(client);
    const int canvas_left = layout.left_width + 24;
    const int canvas_right = layout.width - layout.right_width - 24;
    const int canvas_top = 58 + 56;
    const int canvas_bottom = layout.height - 28 - 48;
    const int center_y = canvas_top + (canvas_bottom - canvas_top) / 2;
    const int button_left = (canvas_left + canvas_right - 264) / 2;
    return x >= button_left + 140 && x < button_left + 264 &&
           y >= center_y + 18 && y < center_y + 62;
}

bool game_type_choice_hit(const int x, const int y, const RECT& client, editor::GameType& type) noexcept {
    // Startup chooser card ONLY. Inspector rows use game_type_row_hit so
    // invisible card areas never steal canvas clicks once closed.
    for (int i = 0; i < 4; ++i) {
        const RECT opt = chooser_option_rect(client, i);
        if (PtInRect(&opt, POINT{x, y})) {
            type = static_cast<editor::GameType>(i);
            return true;
        }
    }
    return false;
}

bool game_type_row_hit(const int x, const int y, const RECT& client, editor::GameType& type) noexcept {
    // Inspector rows (Platformer=0, TopDown=1, Orbit=2, SideScroller=3).
    for (int i = 0; i < 4; ++i) {
        const RECT row = game_type_row_rect(client, i);
        if (PtInRect(&row, POINT{x, y})) {
            type = static_cast<editor::GameType>(i);
            return true;
        }
    }
    return false;
}

bool set_spawn_button_hit(const int x, const int y, const RECT& client) noexcept {
    const RECT rect = set_spawn_rect(client);
    return PtInRect(&rect, POINT{x, y}) != 0;
}

bool object_action_hit(const int x, const int y, const RECT& client) noexcept {
    const RECT rect = object_action_rect(client);
    return PtInRect(&rect, POINT{x, y}) != 0;
}

bool background_button_hit(const int x, const int y, const RECT& client) noexcept {
    const RECT rect = background_rect(client);
    return PtInRect(&rect, POINT{x, y}) != 0;
}

} // namespace ogs::studio

#endif
