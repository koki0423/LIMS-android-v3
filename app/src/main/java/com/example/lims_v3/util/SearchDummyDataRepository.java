package com.example.lims_v3.util;

import com.example.lims_v3.network.AssetMasterResponse;
import com.example.lims_v3.network.AssetResponse;
import com.example.lims_v3.network.AssetSetResponse;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SearchDummyDataRepository {
    public static final String GENRE_ALL = "すべて";

    private SearchDummyDataRepository() {
    }

    public static List<AssetSetResponse> getAssets() {
        List<AssetSetResponse> items = new ArrayList<>();
        items.add(createAsset(
                1L, 101L,
                "ノートPC", "MacBook Pro 14", "Apple", "M3 Pro",
                "PC-2025-001", "情報システム室", 3, 1,
                "MBP14-001", "開発端末"
        ));
        items.add(createAsset(
                2L, 102L,
                "ノートPC", "ThinkPad X1 Carbon", "Lenovo", "Gen 11",
                "PC-2025-014", "貸出棚 A", 2, 4,
                "TPX1-014", "営業部向け共用機"
        ));
        items.add(createAsset(
                3L, 103L,
                "周辺機器", "USB-C ドッキングステーション", "Anker", "PowerExpand 8-in-1",
                "ACC-2025-008", "貸出棚 B", 6, 1,
                "ANK-DOCK-08", "会議室持ち出し可"
        ));
        items.add(createAsset(
                4L, 104L,
                "測定機器", "デジタルマルチメータ", "HIOKI", "DT4256",
                "MES-2024-021", "実験室 2", 4, 1,
                "HIOKI-021", "校正期限: 2026-10"
        ));
        items.add(createAsset(
                5L, 105L,
                "ネットワーク", "Wi-Fi ルーター", "Yamaha", "WLX222",
                "NET-2025-003", "サーバ室", 1, 2,
                "WLX222-003", "設定変更中"
        ));
        items.add(createAsset(
                6L, 106L,
                "周辺機器", "27インチモニター", "Dell", "U2723QE",
                "DIS-2025-011", "執務室 東", 5, 1,
                "U2723-011", "USB-C 給電対応"
        ));
        items.add(createAsset(
                7L, 107L,
                "タブレット", "iPad Air", "Apple", "11-inch M2",
                "TAB-2025-006", "貸出棚 C", 2, 4,
                "IPADAIR-006", "ペンシル別管理"
        ));
        items.add(createAsset(
                8L, 108L,
                "測定機器", "バーコードリーダー", "Honeywell", "Xenon 1950g",
                "BR-2025-002", "受付カウンター", 2, 1,
                "X1950G-002", "返却処理用"
        ));
        return items;
    }

    public static List<String> getGenres(List<AssetSetResponse> items) {
        Set<String> genres = new LinkedHashSet<>();
        genres.add(GENRE_ALL);
        for (AssetSetResponse item : items) {
            if (item == null || item.getMaster() == null) {
                continue;
            }

            String genre = item.getMaster().getGenre();
            if (genre != null && !genre.trim().isEmpty()) {
                genres.add(genre);
            }
        }
        return new ArrayList<>(genres);
    }

    private static AssetSetResponse createAsset(
            long masterId,
            long assetId,
            String genre,
            String name,
            String manufacturer,
            String model,
            String managementNumber,
            String location,
            int quantity,
            int statusId,
            String serial,
            String notes
    ) {
        AssetMasterResponse master = new AssetMasterResponse(masterId, name, manufacturer, model, genre);
        AssetResponse asset = new AssetResponse(
                assetId,
                managementNumber,
                location,
                quantity,
                statusId,
                serial,
                null,
                notes
        );
        return new AssetSetResponse(master, asset);
    }
}
