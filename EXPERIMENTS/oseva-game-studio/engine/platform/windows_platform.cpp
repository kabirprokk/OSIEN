// OGS Engine | version 0.2.0 | Windows platform, intro playback, and Studio shell
#include "platform/platform.h"
#include "editor/studio_shell.h"
#include "editor/document.h"
#include "editor/browser_game.h"
#include "physics/physics_world.h"

#if defined(_WIN32)

#include <windows.h>
#include <windowsx.h>
#include <gdiplus.h>
#include <mfapi.h>
#include <mferror.h>
#include <mfplay.h>
#include <commdlg.h>
#include <shellapi.h>

#include <string>
#include <atomic>
#include <memory>
#include <cmath>
#include <filesystem>
#include <fstream>
#include <cwchar>
#include <cwctype>
#include <exception>
#include <algorithm>
#include <iterator>

namespace ogs {

class WindowsPlatform final : public Platform, public IMFPMediaPlayerCallback {
public:
    explicit WindowsPlatform(Application& application) : application_(application) {}
    ~WindowsPlatform() override { shutdown(); }

    bool initialize(const ApplicationConfig& config) override {
        instance_ = GetModuleHandleW(nullptr);
        SetProcessDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2);
        CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
        com_initialized_ = true;

        Gdiplus::GdiplusStartupInput gdiplus_input{};
        if (Gdiplus::GdiplusStartup(&gdiplus_token_, &gdiplus_input, nullptr) != Gdiplus::Ok) {
            gdiplus_token_ = 0;
        }

        const std::wstring logo_path = asset_path(L"assets\\branding\\ogs_logo_primary.png");
        if (gdiplus_token_ != 0) {
            Gdiplus::Bitmap logo(logo_path.c_str());
            if (logo.GetLastStatus() == Gdiplus::Ok) {
                logo.GetHICON(&icon_);
            }
            const std::wstring sprite_path = asset_path(L"assets\\player\\grid_soul_01.png");
            demo_sprite_ = std::make_unique<Gdiplus::Bitmap>(sprite_path.c_str());
            if (demo_sprite_->GetLastStatus() != Gdiplus::Ok) {
                demo_sprite_.reset();
            }
        }

        demo_body_id_ = physics_.create_body({0, {-150.0F, 0.0F}, {0.0F, -70.0F}, 14.0F, 1.0F, 1.0F, true});
        physics_.add_gravity_well({{0.0F, 0.0F}, 950.0F, 500.0F, false});

        WNDCLASSW window_class{};
        window_class.hInstance = instance_;
        window_class.lpfnWndProc = &WindowsPlatform::window_proc;
        window_class.lpszClassName = L"OGS_ENGINE_WINDOW";
        window_class.hCursor = LoadCursorW(nullptr, IDC_ARROW);
        window_class.hIcon = icon_ != nullptr ? icon_ : LoadIconW(nullptr, IDI_APPLICATION);
        RegisterClassW(&window_class);

        window_ = CreateWindowExW(
            0, window_class.lpszClassName, L"OSEVA Game Studio", WS_OVERLAPPEDWINDOW,
            CW_USEDEFAULT, CW_USEDEFAULT, config.logical_size.width, config.logical_size.height,
            nullptr, nullptr, instance_, this);
        if (window_ == nullptr) {
            return false;
        }

        RECT client{};
        GetClientRect(window_, &client);
        video_window_ = CreateWindowExW(
            0, L"STATIC", nullptr, WS_CHILD | WS_VISIBLE,
            0, 0, client.right, client.bottom,
            window_, nullptr, instance_, nullptr);

        ShowWindow(window_, SW_SHOW);
        UpdateWindow(window_);
        start_intro();
        return true;
    }

    void pump_events(Application& application) override {
        MSG message{};
        while (PeekMessageW(&message, nullptr, 0, 0, PM_REMOVE) != 0) {
            if (message.message == WM_QUIT) {
                application.request_quit();
                continue;
            }
            TranslateMessage(&message);
            DispatchMessageW(&message);
        }
        // Poll-based skip: MFPlay/EVR can swallow key/mouse messages targeted
        // at the video surface, so the WndProc alone is not reliable.
        // Poll here on the main loop so ANY press skips, even with focus issues.
        if (intro_playing_.load()) {
            poll_intro_skip_input();
        }
        const ULONGLONG intro_deadline = intro_deadline_.load();
        if (intro_playing_.load() && intro_deadline != 0 && GetTickCount64() >= intro_deadline) {
            intro_finish_pending_.store(true);
        }
        if (intro_finish_pending_.load()) {
            finish_intro();
        }
    }

    void present(Application&) override {
        MsgWaitForMultipleObjectsEx(0, nullptr, 8, QS_ALLINPUT, MWMO_INPUTAVAILABLE);
    }

    void fixed_update(const float fixed_delta_seconds) override {
        if (!preview_active_) {
            return;
        }
        physics_.step(fixed_delta_seconds);
        animation_time_ += fixed_delta_seconds;
        if (++preview_render_divider_ >= 2) {
            preview_render_divider_ = 0;
            InvalidateRect(window_, nullptr, FALSE);
        }
    }

    void save_project(const bool choose_path) {
        if (!choose_path && !project_path_.empty()) {
            status_ = document_.save(project_path_) ? L"Project saved." : L"Could not save project file.";
            return;
        }
        wchar_t path[MAX_PATH] = L"MyGame.ogsproject";
        OPENFILENAMEW dialog{};
        dialog.lStructSize = sizeof(dialog);
        dialog.hwndOwner = window_;
        dialog.lpstrFilter = L"OGS Studio Project (*.ogsproject)\0*.ogsproject\0All files (*.*)\0*.*\0\0";
        dialog.lpstrFile = path;
        dialog.nMaxFile = MAX_PATH;
        dialog.lpstrDefExt = L"ogsproject";
        dialog.Flags = OFN_OVERWRITEPROMPT | OFN_PATHMUSTEXIST;
        if (GetSaveFileNameW(&dialog)) {
            project_path_ = path;
            status_ = document_.save(project_path_) ? L"Project saved." : L"Could not save project file.";
        }
    }

    void open_project() {
        wchar_t path[MAX_PATH]{};
        OPENFILENAMEW dialog{};
        dialog.lStructSize = sizeof(dialog);
        dialog.hwndOwner = window_;
        dialog.lpstrFilter = L"OGS Studio Project (*.ogsproject)\0*.ogsproject\0All files (*.*)\0*.*\0\0";
        dialog.lpstrFile = path;
        dialog.nMaxFile = MAX_PATH;
        dialog.Flags = OFN_FILEMUSTEXIST | OFN_PATHMUSTEXIST;
        if (GetOpenFileNameW(&dialog)) {
            if (document_.load(path)) {
                project_path_ = path;
                preview_active_ = false;
                show_chooser_ = false;
                status_ = L"Project opened.";
            } else {
                status_ = L"Project file is invalid or unsupported.";
            }
        }
    }

    void run_in_browser() {
        try {
            const std::filesystem::path preview_dir = std::filesystem::temp_directory_path() / L"OSEVA Game Studio" / L"browser-preview";
            std::filesystem::create_directories(preview_dir);
            const std::filesystem::path html_path = preview_dir / L"index.html";
            const std::string html = editor::make_browser_game_html(document_);
            {
                std::ofstream output(html_path, std::ios::binary | std::ios::trunc);
                if (!output || !output.write(html.data(), static_cast<std::streamsize>(html.size())) || !output.flush()) {
                    status_ = L"Could not write the browser preview. Check temporary-folder permissions.";
                    return;
                }
            }
            const HINSTANCE result = ShellExecuteW(window_, L"open", html_path.c_str(), nullptr, preview_dir.c_str(), SW_SHOWNORMAL);
            if (reinterpret_cast<INT_PTR>(result) <= 32) {
                status_ = L"Could not open the browser. Check your Windows HTML/browser association.";
                return;
            }
            status_ = L"Game preview opened in your default browser. Press R there to restart.";
        } catch (const std::filesystem::filesystem_error&) {
            status_ = L"Could not create the browser preview folder.";
        } catch (const std::exception&) {
            status_ = L"Could not generate the browser preview.";
        }
    }

    void add_object_menu() {
        HMENU menu = CreatePopupMenu();
        AppendMenuW(menu, MF_STRING, 1, L"Grid Soul Orb (player)");
        AppendMenuW(menu, MF_STRING, 2, L"Slab (Neon obstacle / bouncy wall)");
        AppendMenuW(menu, MF_STRING, 3, L"Gravity Planet (well)");
        AppendMenuW(menu, MF_STRING, 4, L"Hazard spikes");
        AppendMenuW(menu, MF_STRING, 5, L"Goal Ring (sling)");
        AppendMenuW(menu, MF_STRING, 6, L"Gold Coin (+1, big +5)");
        AppendMenuW(menu, MF_STRING, 7, L"Light Mote (+10)");
        AppendMenuW(menu, MF_STRING, 8, L"Launch Pad (sling start)");
        AppendMenuW(menu, MF_STRING, 9, L"Finish Gate (neon end)");
        POINT point{};
        GetCursorPos(&point);
        const UINT picked = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY,
                                           point.x, point.y, 0, window_, nullptr);
        DestroyMenu(menu);
        if (picked >= 1 && picked <= 9) {
            RECT client{};
            GetClientRect(window_, &client);
            const studio::StudioLayout layout = studio::layout_for(client);
            const auto kind = static_cast<editor::ObjectKind>(picked - 1);
            document_.add(kind, studio::screen_to_world(layout,
                (layout.canvas.left + layout.canvas.right) / 2,
                (layout.canvas.top + layout.canvas.bottom) / 2));
            ensure_selected_in_hierarchy(client);
            preview_active_ = false;
            status_ = L"Object added. Drag it on the canvas to position it.";
            InvalidateRect(window_, nullptr, FALSE);
        }
    }

    void add_template_menu() {
        HMENU menu = CreatePopupMenu();
        AppendMenuW(menu, MF_STRING, 1, L"Neon Pulse starter (flip + coins + gate)");
        AppendMenuW(menu, MF_STRING, 2, L"Orbit Sling starter (pad + well + goal)");
        AppendMenuW(menu, MF_STRING, 3, L"Top-down starter (maze + coin)");
        AppendMenuW(menu, MF_STRING, 4, L"Platformer starter (slabs + gate)");
        POINT point{};
        GetCursorPos(&point);
        const UINT picked = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY,
                                           point.x, point.y, 0, window_, nullptr);
        DestroyMenu(menu);
        if (picked == 0) return;

        const auto add = [this](const editor::ObjectKind kind, const float x, const float y) {
            document_.add(kind, {x, y});
        };
        if (picked == 1) {
            // Neon Pulse: flip through slab gaps, grab gold + motes, hit the gate.
            document_.set_game_type(editor::GameType::SideScroller);
            add(editor::ObjectKind::Orb, -360.0F, 0.0F);
            add(editor::ObjectKind::Platform, -180.0F, 150.0F);
            add(editor::ObjectKind::Platform, -180.0F, -150.0F);
            add(editor::ObjectKind::Platform, 40.0F, 170.0F);
            add(editor::ObjectKind::Platform, 40.0F, -130.0F);
            const auto mover = document_.add(editor::ObjectKind::Platform, {220.0F, 0.0F});
            document_.set_mover(mover, 55.0F, 2.0F);
            add(editor::ObjectKind::Coin, -80.0F, 0.0F);
            add(editor::ObjectKind::Mote, 130.0F, 60.0F);
            add(editor::ObjectKind::Gate, 360.0F, 0.0F);
            status_ = L"Neon Pulse starter added. Tap/Space flips gravity — reach the gate.";
        } else if (picked == 2) {
            // Orbit Sling: drag back from the pad, bend around the well, hit par.
            document_.set_game_type(editor::GameType::OrbitPuzzle);
            add(editor::ObjectKind::Pad, -320.0F, 0.0F);
            add(editor::ObjectKind::Orb, -320.0F, 0.0F);
            add(editor::ObjectKind::Planet, 0.0F, 0.0F);
            add(editor::ObjectKind::Hazard, 160.0F, -120.0F);
            add(editor::ObjectKind::Goal, 320.0F, 100.0F);
            add(editor::ObjectKind::Coin, 120.0F, 120.0F);
            document_.set_par(3);
            document_.set_hint("Bend it like gravity.");
            status_ = L"Orbit Sling starter added. Drag back, release, beat par 3.";
        } else if (picked == 3) {
            document_.set_game_type(editor::GameType::TopDown);
            add(editor::ObjectKind::Orb, -320.0F, 0.0F);
            add(editor::ObjectKind::Platform, -120.0F, 120.0F);
            add(editor::ObjectKind::Platform, 120.0F, -120.0F);
            add(editor::ObjectKind::Platform, 0.0F, 0.0F);
            add(editor::ObjectKind::Hazard, 200.0F, 100.0F);
            add(editor::ObjectKind::Coin, 60.0F, 160.0F);
            add(editor::ObjectKind::Goal, 340.0F, 0.0F);
            status_ = L"Top-down starter added. WASD moves in all 8 directions.";
        } else if (picked == 4) {
            document_.set_game_type(editor::GameType::Platformer);
            add(editor::ObjectKind::Orb, -360.0F, -150.0F);
            add(editor::ObjectKind::Platform, -220.0F, -205.0F);
            add(editor::ObjectKind::Platform, 0.0F, -160.0F);
            add(editor::ObjectKind::Hazard, 20.0F, -120.0F);
            add(editor::ObjectKind::Coin, 110.0F, -60.0F);
            add(editor::ObjectKind::Platform, 220.0F, -100.0F);
            add(editor::ObjectKind::Gate, 360.0F, -40.0F);
            status_ = L"Platformer starter added. Flip, dodge, grab coins, gate wins.";
        }
        RECT client{};
        GetClientRect(window_, &client);
        ensure_selected_in_hierarchy(client);
        preview_active_ = false;
        InvalidateRect(window_, nullptr, FALSE);
    }

    void create_new_level() {
        const auto type = document_.game_type();
        const auto add = [this](const editor::ObjectKind kind, const float x, const float y) {
            document_.add(kind, {x, y});
        };
        if (type == editor::GameType::TopDown) {
            add(editor::ObjectKind::Orb, -320.0F, 0.0F);
            add(editor::ObjectKind::Platform, 0.0F, 120.0F);
            add(editor::ObjectKind::Platform, 0.0F, -120.0F);
            add(editor::ObjectKind::Coin, 120.0F, 0.0F);
            add(editor::ObjectKind::Goal, 300.0F, 0.0F);
            status_ = L"Top-down level created. WASD moves all 8 ways. RUN to play.";
        } else if (type == editor::GameType::OrbitPuzzle) {
            add(editor::ObjectKind::Pad, -320.0F, 0.0F);
            add(editor::ObjectKind::Orb, -320.0F, 0.0F);
            add(editor::ObjectKind::Planet, 0.0F, 0.0F);
            add(editor::ObjectKind::Goal, 300.0F, 100.0F);
            status_ = L"Orbit level created. Drag back + release. RUN to play.";
        } else {
            add(editor::ObjectKind::Orb, -260.0F, 0.0F);
            add(editor::ObjectKind::Platform, 0.0F, 150.0F);
            add(editor::ObjectKind::Platform, 0.0F, -150.0F);
            add(editor::ObjectKind::Coin, 80.0F, 0.0F);
            add(editor::ObjectKind::Gate, 300.0F, 0.0F);
            status_ = L"Neon level created. Tap/Space flips. RUN to play.";
        }
        RECT client{};
        GetClientRect(window_, &client);
        ensure_selected_in_hierarchy(client);
        preview_active_ = false;
        InvalidateRect(window_, nullptr, FALSE);
    }

    void level_menu() {
        HMENU menu = CreatePopupMenu();
        HMENU par_menu = CreatePopupMenu();
        AppendMenuW(par_menu, MF_STRING, 11, L"Par 2 (easy)");
        AppendMenuW(par_menu, MF_STRING, 12, L"Par 3 (normal)");
        AppendMenuW(par_menu, MF_STRING, 13, L"Par 5 (hard)");
        AppendMenuW(menu, MF_POPUP, reinterpret_cast<UINT_PTR>(par_menu), L"Set par (sling stars)");
        AppendMenuW(menu, MF_SEPARATOR, 0, nullptr);
        AppendMenuW(menu, MF_STRING, 21, L"Hint: Reach the golden goal.");
        AppendMenuW(menu, MF_STRING, 22, L"Hint: Bend it like gravity.");
        AppendMenuW(menu, MF_STRING, 23, L"Hint: Thread the gap. Gentle pulls win.");
        AppendMenuW(menu, MF_STRING, 24, L"Hint: Less pull, more curve.");
        AppendMenuW(menu, MF_STRING, 25, L"Hint: Tap to flip, dodge the slabs.");
        POINT point{};
        GetCursorPos(&point);
        const UINT picked = TrackPopupMenu(menu, TPM_RETURNCMD | TPM_NONOTIFY,
                                           point.x, point.y, 0, window_, nullptr);
        DestroyMenu(par_menu);
        DestroyMenu(menu);
        switch (picked) {
            case 11: document_.set_par(2); status_ = L"Par 2: generous 3-star window."; break;
            case 12: document_.set_par(3); status_ = L"Par 3: classic Orbit Sling par."; break;
            case 13: document_.set_par(5); status_ = L"Par 5: hard puzzle par."; break;
            case 21: document_.set_hint("Reach the golden goal."); status_ = L"Hint set."; break;
            case 22: document_.set_hint("Bend it like gravity."); status_ = L"Hint set."; break;
            case 23: document_.set_hint("Thread the gap. Gentle pulls win."); status_ = L"Hint set."; break;
            case 24: document_.set_hint("Less pull, more curve."); status_ = L"Hint set."; break;
            case 25: document_.set_hint("Tap to flip, dodge the slabs."); status_ = L"Hint set."; break;
            default: return;
        }
        preview_active_ = false;
        InvalidateRect(window_, nullptr, FALSE);
    }

    void apply_game_type_selection(const editor::GameType type) {
        document_.set_game_type(type);
        show_chooser_ = false;
        preview_active_ = false;
        switch (type) {
            case editor::GameType::Platformer: status_ = L"Platformer selected. Run + jump with WASD / arrows."; break;
            case editor::GameType::TopDown: status_ = L"Top-down selected. 8-way movement, no gravity."; break;
            case editor::GameType::OrbitPuzzle: status_ = L"Orbit puzzle selected. Planets pull you."; break;
            case editor::GameType::SideScroller: status_ = L"Side-scroller selected. Time your jumps."; break;
        }
        InvalidateRect(window_, nullptr, FALSE);
    }

    void pick_background_color() {
        const std::uint32_t current = document_.background_color();
        CHOOSECOLORW chooser{};
        chooser.lStructSize = sizeof(chooser);
        chooser.hwndOwner = window_;
        chooser.rgbResult = RGB((current >> 16) & 0xFF, (current >> 8) & 0xFF, current & 0xFF);
        chooser.lpCustColors = custom_colors_;
        chooser.Flags = CC_FULLOPEN | CC_RGBINIT;
        if (ChooseColorW(&chooser)) {
            const std::uint32_t rgb = (static_cast<std::uint32_t>(GetRValue(chooser.rgbResult)) << 16) |
                (static_cast<std::uint32_t>(GetGValue(chooser.rgbResult)) << 8) |
                static_cast<std::uint32_t>(GetBValue(chooser.rgbResult));
            document_.set_background_color(rgb);
            status_ = L"Background color changed. RUN shows the same color.";
            InvalidateRect(window_, nullptr, FALSE);
        }
    }

    std::uint32_t hit_test_object(const int x, const int y, const RECT& client) const {
        const studio::StudioLayout layout = studio::layout_for(client);
        if (!PtInRect(&layout.canvas, POINT{x, y})) return 0;
        const float center_x = (layout.canvas.left + layout.canvas.right) * 0.5F;
        const float center_y = (layout.canvas.top + layout.canvas.bottom) * 0.5F;
        float best_distance = 30.0F * 30.0F;
        std::uint32_t best_id = 0;
        for (const editor::SceneObject& object : document_.objects()) {
            const float sx = center_x + object.position.x * layout.canvas_scale;
            const float sy = center_y - object.position.y * layout.canvas_scale;
            const float dx = x - sx;
            const float dy = y - sy;
            const float distance = dx * dx + dy * dy;
            const float radius = std::max(12.0F, 24.0F * object.scale * layout.canvas_scale);
            if (distance <= radius * radius && distance < best_distance) {
                best_distance = distance;
                best_id = object.id;
            }
        }
        return best_id;
    }

    void shutdown() noexcept override {
        stop_intro();
        if (window_ != nullptr) {
            DestroyWindow(window_);
            window_ = nullptr;
        }
        if (icon_ != nullptr) {
            DestroyIcon(icon_);
            icon_ = nullptr;
        }
        if (gdiplus_token_ != 0) {
            Gdiplus::GdiplusShutdown(gdiplus_token_);
            gdiplus_token_ = 0;
        }
        if (com_initialized_) {
            CoUninitialize();
            com_initialized_ = false;
        }
    }

    HRESULT STDMETHODCALLTYPE QueryInterface(REFIID riid, void** object) override {
        if (object == nullptr) {
            return E_POINTER;
        }
        *object = nullptr;
        if (riid == __uuidof(IUnknown) || riid == __uuidof(IMFPMediaPlayerCallback)) {
            *object = static_cast<IMFPMediaPlayerCallback*>(this);
            AddRef();
            return S_OK;
        }
        return E_NOINTERFACE;
    }

    ULONG STDMETHODCALLTYPE AddRef() override { return ++reference_count_; }

    ULONG STDMETHODCALLTYPE Release() override {
        return reference_count_ > 0 ? --reference_count_ : 0;
    }

    void STDMETHODCALLTYPE OnMediaPlayerEvent(MFP_EVENT_HEADER* event_header) override {
        if (event_header == nullptr) {
            return;
        }
        if (event_header->eEventType == MFP_EVENT_TYPE_MEDIAITEM_CREATED) {
            // Playback is started automatically by MFPCreateMediaPlayer(TRUE, ...).
            // Do not mutate or shut down the player from this callback thread.
        } else if (event_header->eEventType == MFP_EVENT_TYPE_PLAY) {
            intro_deadline_.store(GetTickCount64() + 7000);
        } else if (event_header->eEventType == MFP_EVENT_TYPE_PLAYBACK_ENDED) {
            intro_finish_pending_.store(true);
        } else if (event_header->eEventType == MFP_EVENT_TYPE_ERROR) {
            intro_finish_pending_.store(true);
        }
    }

private:
    static constexpr UINT kFinishInspectorEdit = WM_APP + 42;
    static constexpr int kInspectorEditControlId = 4107;

    static LRESULT CALLBACK inspector_edit_proc(HWND edit, UINT message, WPARAM w_param, LPARAM l_param) {
        auto* platform = reinterpret_cast<WindowsPlatform*>(GetWindowLongPtrW(edit, GWLP_USERDATA));
        if (platform != nullptr && message == WM_KEYDOWN) {
            if (w_param == VK_RETURN) {
                PostMessageW(platform->window_, kFinishInspectorEdit, TRUE, reinterpret_cast<LPARAM>(edit));
                return 0;
            }
            if (w_param == VK_ESCAPE) {
                PostMessageW(platform->window_, kFinishInspectorEdit, FALSE, reinterpret_cast<LPARAM>(edit));
                return 0;
            }
        }
        if (platform != nullptr && message == WM_KILLFOCUS && edit == platform->inspector_edit_) {
            PostMessageW(platform->window_, kFinishInspectorEdit, TRUE, reinterpret_cast<LPARAM>(edit));
        }
        return platform != nullptr && platform->inspector_original_proc_ != nullptr
            ? CallWindowProcW(platform->inspector_original_proc_, edit, message, w_param, l_param)
            : DefWindowProcW(edit, message, w_param, l_param);
    }

    static bool parse_float(const std::wstring& text, float& result) {
        wchar_t* end = nullptr;
        const float parsed = std::wcstof(text.c_str(), &end);
        if (end == text.c_str()) return false;
        while (*end != L'\0' && std::iswspace(*end)) ++end;
        if (*end != L'\0' || !std::isfinite(parsed)) return false;
        result = parsed;
        return true;
    }

    void begin_inspector_edit(const studio::InspectorField field) {
        if (inspector_edit_ != nullptr) finish_inspector_edit(true);
        const editor::SceneObject* object = document_.find(document_.selected_id());
        if (object == nullptr) return;

        inspector_field_ = field;
        inspector_object_id_ = object->id;
        std::wstring initial;
        if (field == studio::InspectorField::Name) {
            const int length = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, object->name.data(),
                                                   static_cast<int>(object->name.size()), nullptr, 0);
            if (length > 0) {
                initial.resize(static_cast<std::size_t>(length));
                MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, object->name.data(),
                                    static_cast<int>(object->name.size()), initial.data(), length);
            }
        } else {
            wchar_t value[48]{};
            const float number = field == studio::InspectorField::PositionX ? object->position.x
                : field == studio::InspectorField::PositionY ? object->position.y : object->scale;
            swprintf_s(value, L"%.3f", number);
            initial = value;
        }

        RECT client{};
        GetClientRect(window_, &client);
        const RECT bounds = studio::inspector_field_rect(client, field);
        inspector_edit_ = CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", initial.c_str(),
            WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL, bounds.left + 2, bounds.top + 1,
            std::max(1L, bounds.right - bounds.left - 4), std::max(1L, bounds.bottom - bounds.top - 2),
            window_, reinterpret_cast<HMENU>(static_cast<INT_PTR>(kInspectorEditControlId)), instance_, nullptr);
        if (inspector_edit_ == nullptr) {
            status_ = L"Could not open the Inspector field for editing.";
            return;
        }
        SetWindowLongPtrW(inspector_edit_, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(this));
        inspector_original_proc_ = reinterpret_cast<WNDPROC>(SetWindowLongPtrW(
            inspector_edit_, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(&WindowsPlatform::inspector_edit_proc)));
        SendMessageW(inspector_edit_, EM_SETSEL, 0, -1);
        SetFocus(inspector_edit_);
    }

    void finish_inspector_edit(const bool commit) {
        HWND edit = inspector_edit_;
        if (edit == nullptr) return;
        inspector_edit_ = nullptr;
        std::wstring value;
        if (commit) {
            const int length = GetWindowTextLengthW(edit);
            value.resize(static_cast<std::size_t>(std::max(0, length)) + 1);
            if (length > 0) GetWindowTextW(edit, value.data(), length + 1);
            value.resize(static_cast<std::size_t>(std::max(0, length)));
        }
        DestroyWindow(edit);
        inspector_original_proc_ = nullptr;
        if (!commit) {
            InvalidateRect(window_, nullptr, FALSE);
            return;
        }

        editor::SceneObject* object = document_.find(inspector_object_id_);
        if (object == nullptr) return;
        bool changed = false;
        if (inspector_field_ == studio::InspectorField::Name) {
            const int bytes = WideCharToMultiByte(CP_UTF8, WC_ERR_INVALID_CHARS, value.c_str(), -1,
                                                   nullptr, 0, nullptr, nullptr);
            if (bytes > 1 && bytes <= 130) {
                std::string utf8(static_cast<std::size_t>(bytes), '\0');
                WideCharToMultiByte(CP_UTF8, WC_ERR_INVALID_CHARS, value.c_str(), -1,
                                    utf8.data(), bytes, nullptr, nullptr);
                utf8.pop_back();
                if (utf8 != object->name) {
                    document_.begin_edit();
                    changed = document_.rename(object->id, std::move(utf8));
                }
            }
        } else {
            float parsed = 0.0F;
            if (parse_float(value, parsed)) {
                if (inspector_field_ == studio::InspectorField::PositionX ||
                    inspector_field_ == studio::InspectorField::PositionY) {
                    Vec2 position = object->position;
                    if (inspector_field_ == studio::InspectorField::PositionX) position.x = parsed;
                    else position.y = parsed;
                    if (position.x != object->position.x || position.y != object->position.y) {
                        document_.begin_edit();
                        changed = document_.move(object->id, position);
                    }
                } else if (parsed != object->scale) {
                    document_.begin_edit();
                    changed = document_.set_scale(object->id, parsed);
                }
            }
        }
        status_ = changed ? L"Inspector change applied." : L"No valid Inspector change was made.";
        InvalidateRect(window_, nullptr, FALSE);
    }

    void scroll_hierarchy(const int delta, const RECT& client) {
        const studio::StudioLayout layout = studio::layout_for(client);
        const int top = 58 + 122;
        const int bottom = layout.height - 28 - 82;
        const int visible = std::max(1, (bottom - top) / 28);
        const int maximum = std::max(0, static_cast<int>(document_.objects().size()) - visible);
        hierarchy_scroll_ = std::clamp(hierarchy_scroll_ + delta, 0, maximum);
        InvalidateRect(window_, nullptr, FALSE);
    }

    void ensure_selected_in_hierarchy(const RECT& client) {
        const auto& objects = document_.objects();
        const auto found = std::find_if(objects.begin(), objects.end(), [this](const editor::SceneObject& object) {
            return object.id == document_.selected_id();
        });
        if (found == objects.end()) return;
        const studio::StudioLayout layout = studio::layout_for(client);
        const int visible = std::max(1, (layout.height - 28 - 82 - (58 + 122)) / 28);
        const int index = static_cast<int>(std::distance(objects.begin(), found));
        if (index < hierarchy_scroll_) hierarchy_scroll_ = index;
        else if (index >= hierarchy_scroll_ + visible) hierarchy_scroll_ = index - visible + 1;
    }

    std::wstring project_name() const {
        if (project_path_.empty()) return L"UNTITLED PROJECT";
        std::wstring name = project_path_;
        const std::size_t slash = name.find_last_of(L"\\/");
        if (slash != std::wstring::npos) name = name.substr(slash + 1);
        const std::size_t dot = name.rfind(L'.');
        if (dot != std::wstring::npos) name = name.substr(0, dot);
        for (wchar_t& ch : name) ch = static_cast<wchar_t>(towupper(ch));
        return name.empty() ? L"UNTITLED PROJECT" : name;
    }

    static std::wstring asset_path(const wchar_t* relative_path) {
        wchar_t module_path[MAX_PATH]{};
        GetModuleFileNameW(nullptr, module_path, MAX_PATH);
        std::wstring path(module_path);
        const std::size_t slash = path.find_last_of(L"\\/");
        path.resize(slash == std::wstring::npos ? 0 : slash);
        path += L"\\";
        path += relative_path;
        return path;
    }

    void start_intro() {
        if (FAILED(MFStartup(MF_VERSION))) {
            destroy_video_window();
            return;
        }
        mf_started_ = true;
        const std::wstring intro_path = asset_path(L"assets\\branding\\ogs_intro.mp4");
        const HRESULT result = MFPCreateMediaPlayer(
            intro_path.c_str(), TRUE, MFP_OPTION_NONE, this, video_window_, &media_player_);
        if (FAILED(result)) {
            MFShutdown();
            mf_started_ = false;
            intro_playing_.store(false);
            destroy_video_window();
            status_ = L"Intro video could not load; continuing to Studio.";
            InvalidateRect(window_, nullptr, FALSE);
            return;
        }
        intro_playing_.store(true);
        intro_finish_pending_.store(false);
        // Fail open if Media Foundation does not report playback start.
        intro_deadline_.store(GetTickCount64() + 15000);
    }

    void request_intro_finish() noexcept {
        // Called from WndProc input paths. NEVER tear down MFPlay here:
        // teardown must run on the main loop to avoid freezing on the
        // last video frame (EVR presenter deadlock).
        intro_finish_pending_.store(true);
    }

    void poll_intro_skip_input() {
        // Any fresh key or mouse press skips. Use the low bit transition
        // (GetAsyncKeyState & 1) so holding a key does not retrigger, and so
        // skip input does not leak into the Studio as a held click.
        // Check mouse buttons.
        static const int kMouseButtons[] = {VK_LBUTTON, VK_RBUTTON, VK_MBUTTON, VK_XBUTTON1, VK_XBUTTON2};
        for (int button : kMouseButtons) {
            if ((GetAsyncKeyState(button) & 1) != 0) {
                request_intro_finish();
                return;
            }
        }
        // Check keyboard: any virtual key 0x08..0xFE freshly pressed.
        for (int key = 0x08; key <= 0xFE; ++key) {
            if ((GetAsyncKeyState(key) & 1) != 0) {
                request_intro_finish();
                return;
            }
        }
    }

    void stop_intro() noexcept {
        if (media_player_ != nullptr) {
            IMFPMediaPlayer* player = media_player_;
            media_player_ = nullptr;
            // Best-effort teardown; never let a COM failure freeze the Studio.
            player->Stop();
            player->ClearMediaItem();
            player->Shutdown();
            player->Release();
            if (mf_started_) {
                MFShutdown();
                mf_started_ = false;
            }
        }
        if (video_window_ != nullptr) {
            DestroyWindow(video_window_);
            video_window_ = nullptr;
        }
        intro_playing_.store(false);
        intro_deadline_.store(0);
    }

    void destroy_video_window() noexcept {
        if (video_window_ != nullptr) {
            DestroyWindow(video_window_);
            video_window_ = nullptr;
        }
    }

    void finish_intro() {
        // Main-loop only. Safe to call repeatedly.
        if (!intro_playing_.load() && !intro_finish_pending_.load()) {
            return;
        }
        intro_finish_pending_.store(false);
        const bool was_playing = intro_playing_.load();
        if (was_playing) {
            // Hide first so the last EVR frame is removed even if the
            // presenter holds one queued frame.
            ShowWindow(window_, SW_HIDE);
            stop_intro();
            ShowWindow(window_, SW_SHOW);
            SetForegroundWindow(window_);
            SetFocus(window_);
            // Record finish time so the click/key that skipped does not
            // leak into the Studio canvas as a new action.
            intro_finished_tick_ = GetTickCount64();
            InvalidateRect(window_, nullptr, TRUE);
            RedrawWindow(window_, nullptr, nullptr,
                         RDW_ERASE | RDW_INVALIDATE | RDW_UPDATENOW | RDW_ALLCHILDREN);
            UpdateWindow(window_);
        }
    }

    static LRESULT CALLBACK window_proc(HWND window, UINT message, WPARAM w_param, LPARAM l_param) {
        if (message == WM_NCCREATE) {
            const auto* create = reinterpret_cast<CREATESTRUCTW*>(l_param);
            SetWindowLongPtrW(window, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(create->lpCreateParams));
        }

        auto* platform = reinterpret_cast<WindowsPlatform*>(GetWindowLongPtrW(window, GWLP_USERDATA));
        if (platform != nullptr && platform->intro_playing_.load()) {
            switch (message) {
                case WM_KEYDOWN:
                case WM_SYSKEYDOWN:
                case WM_KEYUP:
                case WM_SYSKEYUP:
                case WM_CHAR:
                case WM_LBUTTONDOWN:
                case WM_LBUTTONUP:
                case WM_RBUTTONDOWN:
                case WM_RBUTTONUP:
                case WM_MBUTTONDOWN:
                case WM_MBUTTONUP:
                case WM_XBUTTONDOWN:
                case WM_XBUTTONUP:
                case WM_POINTERDOWN:
                case WM_POINTERUP:
                case WM_TOUCH:
                case WM_MOUSEWHEEL:
                case WM_MOUSEHWHEEL:
                case WM_APPCOMMAND:
                    // Defer teardown to pump_events; just flag and swallow
                    // so the skip press never reaches the Studio UI.
                    platform->request_intro_finish();
                    return 0;
                default:
                    break;
            }
        }
        if (message == WM_ERASEBKGND) {
            return 1;
        }
        if (message == WM_SIZE && platform != nullptr && platform->video_window_ != nullptr) {
            MoveWindow(platform->video_window_, 0, 0, LOWORD(l_param), HIWORD(l_param), TRUE);
            InvalidateRect(window, nullptr, FALSE);
            return 0;
        }
        if (message == WM_SIZE && platform != nullptr) {
            if (platform->inspector_edit_ != nullptr) {
                RECT client{};
                GetClientRect(window, &client);
                const RECT bounds = studio::inspector_field_rect(client, platform->inspector_field_);
                MoveWindow(platform->inspector_edit_, bounds.left + 2, bounds.top + 1,
                           std::max(1L, bounds.right - bounds.left - 4),
                           std::max(1L, bounds.bottom - bounds.top - 2), TRUE);
            }
            InvalidateRect(window, nullptr, FALSE);
            return 0;
        }
        if (message == kFinishInspectorEdit && platform != nullptr) {
            if (reinterpret_cast<HWND>(l_param) == platform->inspector_edit_) {
                platform->finish_inspector_edit(w_param != FALSE);
            }
            return 0;
        }
        if (message == WM_MOUSEWHEEL && platform != nullptr && !platform->intro_playing_.load()) {
            POINT point{GET_X_LPARAM(l_param), GET_Y_LPARAM(l_param)};
            ScreenToClient(window, &point);
            RECT client{};
            GetClientRect(window, &client);
            if (point.x < studio::layout_for(client).left_width) {
                const int notches = GET_WHEEL_DELTA_WPARAM(w_param) / WHEEL_DELTA;
                if (notches != 0) platform->scroll_hierarchy(-notches * 3, client);
                return 0;
            }
        }
        if (message == WM_GETMINMAXINFO) {
            auto* limits = reinterpret_cast<MINMAXINFO*>(l_param);
            limits->ptMinTrackSize.x = 1000;
            limits->ptMinTrackSize.y = 680;
            return 0;
        }
        if (message == WM_DPICHANGED) {
            const auto* suggested = reinterpret_cast<const RECT*>(l_param);
            SetWindowPos(window, nullptr,
                         suggested->left, suggested->top,
                         suggested->right - suggested->left,
                         suggested->bottom - suggested->top,
                         SWP_NOZORDER | SWP_NOACTIVATE);
            InvalidateRect(window, nullptr, TRUE);
            return 0;
        }
        // MFPlay owns the window paint cycle while the real intro video is active.
        // Intercepting WM_PAINT here causes the video surface to be replaced by the fallback shell.
        if (message == WM_PAINT && platform != nullptr && platform->intro_playing_.load()) {
            return DefWindowProcW(window, message, w_param, l_param);
        }
        if (message == WM_PAINT) {
            PAINTSTRUCT paint{};
            const HDC dc = BeginPaint(window, &paint);
            RECT client{};
            GetClientRect(window, &client);
            const int width = client.right - client.left;
            const int height = client.bottom - client.top;
            HDC buffer_dc = CreateCompatibleDC(dc);
            HBITMAP buffer_bitmap = width > 0 && height > 0 ? CreateCompatibleBitmap(dc, width, height) : nullptr;
            HGDIOBJ previous_bitmap = buffer_bitmap != nullptr ? SelectObject(buffer_dc, buffer_bitmap) : nullptr;
            HDC target_dc = buffer_bitmap != nullptr ? buffer_dc : dc;

            static const ogs::editor::Document empty_document{};
            ogs::studio::draw(target_dc, client, platform != nullptr ? platform->status_ : L"Ready",
                              platform != nullptr ? platform->document_ : empty_document,
                              platform != nullptr ? platform->hierarchy_scroll_ : 0,
                              platform != nullptr ? platform->show_chooser_ : false,
                              platform != nullptr ? platform->project_name() : L"UNTITLED PROJECT",
                              platform != nullptr ? platform->snap_ : true);
            if (platform != nullptr && platform->preview_active_) {
                platform->draw_preview_sprite(target_dc, client);
            }
            if (buffer_bitmap != nullptr) {
                BitBlt(dc, 0, 0, width, height, buffer_dc, 0, 0, SRCCOPY);
                SelectObject(buffer_dc, previous_bitmap);
                DeleteObject(buffer_bitmap);
            }
            if (buffer_dc != nullptr) {
                DeleteDC(buffer_dc);
            }
            EndPaint(window, &paint);
            return 0;
        }

        if (message == WM_LBUTTONDOWN && platform != nullptr && !platform->intro_playing_.load()) {
            if (platform->show_chooser_) return 0;
            if (platform->inspector_edit_ != nullptr) platform->finish_inspector_edit(true);
            RECT client{};
            GetClientRect(window, &client);
            const auto id = platform->hit_test_object(GET_X_LPARAM(l_param), GET_Y_LPARAM(l_param), client);
            if (id != 0) {
                platform->document_.select(id);
                platform->ensure_selected_in_hierarchy(client);
                platform->document_.begin_edit();
                platform->dragging_object_id_ = id;
                SetCapture(window);
                InvalidateRect(window, nullptr, FALSE);
                return 0;
            }
        }
        if (message == WM_MOUSEMOVE && platform != nullptr && platform->dragging_object_id_ != 0) {
            RECT client{};
            GetClientRect(window, &client);
            Vec2 world = studio::screen_to_world(studio::layout_for(client), GET_X_LPARAM(l_param), GET_Y_LPARAM(l_param));
            if (platform->snap_) {
                world.x = std::round(world.x / 8.0F) * 8.0F;
                world.y = std::round(world.y / 8.0F) * 8.0F;
            }
            platform->document_.move(platform->dragging_object_id_, world);
            InvalidateRect(window, nullptr, FALSE);
            return 0;
        }
        if (message == WM_LBUTTONUP && platform != nullptr && platform->dragging_object_id_ != 0) {
            platform->dragging_object_id_ = 0;
            ReleaseCapture();
            return 0;
        }
        if (message == WM_KEYDOWN && platform != nullptr && !platform->intro_playing_.load()) {
            const bool control = (GetKeyState(VK_CONTROL) & 0x8000) != 0;
            if (control && w_param == 'S') { platform->save_project(platform->project_path_.empty()); return 0; }
            if (control && w_param == 'O') { platform->open_project(); InvalidateRect(window, nullptr, FALSE); return 0; }
            if (control && w_param == 'N') { platform->show_chooser_ = true; platform->status_ = L"Choose what type of 2D game to make."; InvalidateRect(window, nullptr, FALSE); return 0; }
            if (control && w_param == 'Z') { platform->document_.undo(); InvalidateRect(window, nullptr, FALSE); return 0; }
            if (control && w_param == 'Y') { platform->document_.redo(); InvalidateRect(window, nullptr, FALSE); return 0; }
            if (control && w_param == 'D') { platform->document_.duplicate(platform->document_.selected_id()); RECT client{}; GetClientRect(window, &client); platform->ensure_selected_in_hierarchy(client); InvalidateRect(window, nullptr, FALSE); return 0; }
            if (w_param == VK_ESCAPE && platform->show_chooser_) { platform->show_chooser_ = false; InvalidateRect(window, nullptr, FALSE); return 0; }
            if (w_param == VK_DELETE) { platform->document_.erase(platform->document_.selected_id()); InvalidateRect(window, nullptr, FALSE); return 0; }
            if (w_param == VK_F5) { platform->run_in_browser(); InvalidateRect(window, nullptr, FALSE); return 0; }
            if (w_param == 'L' && !control) { platform->level_menu(); InvalidateRect(window, nullptr, FALSE); return 0; }
            if (w_param == 'G' && !control) {
                platform->snap_ = !platform->snap_;
                platform->status_ = platform->snap_ ? L"Snap ON: objects land on an 8px grid." : L"Snap OFF: free placement.";
                InvalidateRect(window, nullptr, FALSE);
                return 0;
            }
            if (w_param == VK_OEM_PLUS || w_param == VK_ADD || w_param == VK_OEM_MINUS || w_param == VK_SUBTRACT) {
                auto* selected = platform->document_.find(platform->document_.selected_id());
                if (selected != nullptr) {
                    platform->document_.begin_edit();
                    const float delta = (w_param == VK_OEM_PLUS || w_param == VK_ADD) ? 0.1F : -0.1F;
                    platform->document_.set_scale(selected->id, selected->scale + delta);
                    InvalidateRect(window, nullptr, FALSE);
                }
                return 0;
            }
        }
        if (message == WM_LBUTTONUP && platform != nullptr && !platform->intro_playing_.load()) {
            // Swallow the mouse-up that finished <150ms after the intro so the
            // skip click does not activate a Studio button (input-leak guard).
            const ULONGLONG now = GetTickCount64();
            if (platform->intro_finished_tick_ != 0 && now - platform->intro_finished_tick_ < 150) {
                return 0;
            }
            RECT client{};
            GetClientRect(window, &client);
            const int x = GET_X_LPARAM(l_param);
            const int y = GET_Y_LPARAM(l_param);
            const studio::StudioLayout layout = studio::layout_for(client);
            studio::InspectorField field{};
            editor::GameType picked_type{};
            if (platform->show_chooser_) {
                if (studio::game_type_choice_hit(x, y, client, picked_type)) {
                    platform->apply_game_type_selection(picked_type);
                }
                InvalidateRect(window, nullptr, FALSE);
                return 0;
            }
            const std::uint32_t sidebar_id = studio::sidebar_object_hit(
                x, y, client, platform->document_, platform->hierarchy_scroll_);
            if (studio::game_type_row_hit(x, y, client, picked_type)) {
                platform->document_.set_game_type(picked_type);
                platform->status_ = L"Game type switched. RUN uses the new controls.";
            } else if (studio::set_spawn_button_hit(x, y, client)) {
                const std::uint32_t selected = platform->document_.selected_id();
                if (selected != 0 && platform->document_.set_player_spawn(selected)) {
                    platform->status_ = L"Player spawn set. RUN starts there.";
                } else {
                    platform->status_ = L"Select an ORB (player) first, then SET SPAWN.";
                }
            } else if (studio::object_action_hit(x, y, client)) {
                const std::uint32_t selected = platform->document_.selected_id();
                const char* message = selected != 0 ? platform->document_.cycle_object_action(selected) : nullptr;
                if (message != nullptr) {
                    int needed = MultiByteToWideChar(CP_UTF8, 0, message, -1, nullptr, 0);
                    std::wstring wide(static_cast<std::size_t>(needed > 0 ? needed : 1), L'\0');
                    if (needed > 0) MultiByteToWideChar(CP_UTF8, 0, message, -1, wide.data(), needed);
                    platform->status_ = wide.c_str();
                } else {
                    platform->status_ = L"Select a well, slab or coin to tune it.";
                }
            } else if (studio::background_button_hit(x, y, client)) {
                platform->pick_background_color();
            } else if (studio::inspector_field_hit(x, y, client, field) && platform->document_.selected_id() != 0) {
                platform->begin_inspector_edit(field);
            } else if (sidebar_id != 0) {
                platform->document_.select(sidebar_id);
                platform->ensure_selected_in_hierarchy(client);
                platform->status_ = L"Object selected from the scene hierarchy.";
            } else if (x < layout.left_width && y > layout.height - 28 - 86) {
                platform->add_object_menu();
            } else if (y < 58 && x >= layout.width - 458 && x < layout.width - 384) {
                platform->document_.undo();
            } else if (y < 58 && x >= layout.width - 376 && x < layout.width - 302) {
                platform->document_.redo();
            } else if (y < 58 && x >= layout.width - 294 && x < layout.width - 208) {
                platform->save_project(platform->project_path_.empty());
            } else if (y < 58 && x >= layout.width - 198 && x < layout.width - 20) {
                platform->run_in_browser();
            } else if (platform->document_.objects().empty() && ogs::studio::template_button_hit(x, y, client)) {
                platform->add_template_menu();
            } else if (platform->document_.objects().empty() && ogs::studio::click(x, y, client, platform->status_)) {
                platform->create_new_level();
            }
            InvalidateRect(window, nullptr, FALSE);
            return 0;
        }

        if (message == WM_CLOSE) {
            DestroyWindow(window);
            return 0;
        }
        if (message == WM_DESTROY) {
            PostQuitMessage(0);
            return 0;
        }
        return DefWindowProcW(window, message, w_param, l_param);
    }

    void draw_preview_sprite(HDC dc, const RECT& client) {
        if (demo_sprite_ == nullptr) {
            return;
        }
        const studio::StudioLayout layout = studio::layout_for(client);
        const RECT& canvas = layout.canvas;
        const auto* body = physics_.body(demo_body_id_);
        if (body == nullptr) {
            return;
        }
        const float breathe = 1.0F + std::sin(animation_time_ * 3.2F) * 0.04F;
        const float scale = layout.canvas_scale;
        const int x = (canvas.left + canvas.right) / 2 + static_cast<int>(body->position.x * scale);
        const int y = (canvas.top + canvas.bottom) / 2 - static_cast<int>(body->position.y * scale);
        const int radius = static_cast<int>(28.0F * breathe);

        Gdiplus::Graphics graphics(dc);
        graphics.SetSmoothingMode(Gdiplus::SmoothingModeAntiAlias);
        Gdiplus::SolidBrush aura(Gdiplus::Color(92, 0, 240, 255));
        graphics.FillEllipse(&aura, x - radius - 10, y - radius - 10, (radius + 10) * 2, (radius + 10) * 2);
        Gdiplus::Pen ring(Gdiplus::Color(245, 245, 242, 255), 2.0F);
        graphics.DrawEllipse(&ring, x - radius, y - radius, radius * 2, radius * 2);
        graphics.DrawImage(demo_sprite_.get(), x - radius + 4, y - radius + 4, radius * 2 - 8, radius * 2 - 8);
    }

    Application& application_;
    HINSTANCE instance_ = nullptr;
    HWND window_ = nullptr;
    HWND video_window_ = nullptr;
    HICON icon_ = nullptr;
    ULONG_PTR gdiplus_token_ = 0;
    bool com_initialized_ = false;
    bool mf_started_ = false;
    std::atomic_bool intro_playing_{false};
    std::atomic_bool intro_finish_pending_{false};
    std::atomic<ULONGLONG> intro_deadline_{0};
    ULONGLONG intro_finished_tick_ = 0;
    std::wstring status_ = L"Welcome. Choose a starting point.";
    editor::Document document_;
    std::wstring project_path_;
    HWND inspector_edit_ = nullptr;
    WNDPROC inspector_original_proc_ = nullptr;
    studio::InspectorField inspector_field_ = studio::InspectorField::Name;
    std::uint32_t inspector_object_id_ = 0;
    int hierarchy_scroll_ = 0;
    std::uint32_t dragging_object_id_ = 0;
    bool show_chooser_ = true;
    bool snap_ = true;
    COLORREF custom_colors_[16]{};
    physics::PhysicsWorld physics_;
    physics::BodyId demo_body_id_ = 0;
    std::unique_ptr<Gdiplus::Bitmap> demo_sprite_;
    bool preview_active_ = false;
    float animation_time_ = 0.0F;
    unsigned int preview_render_divider_ = 0;
    ULONG reference_count_ = 1;
    IMFPMediaPlayer* media_player_ = nullptr;
};

Platform* create_windows_platform(Application& application) {
    return new WindowsPlatform(application);
}

} // namespace ogs

#endif

