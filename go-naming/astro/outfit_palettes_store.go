package astro

import (
	"encoding/json"
	"os"
	"sort"
	"strings"
	"sync"
)

var outfitPaletteStoreFile = "outfit_day_palettes.json"
var outfitPaletteStoreOnce sync.Once
var outfitPaletteStoreMu sync.RWMutex
var outfitPaletteStore = defaultOutfitDayPalettes()

type OutfitDayPalettesPayload struct {
	DayPalettes map[string][]string `json:"day_palettes"`
}

func ensureOutfitPaletteStoreLoaded() {
	outfitPaletteStoreOnce.Do(func() {
		data, err := os.ReadFile(outfitPaletteStoreFile)
		if err != nil {
			return
		}
		var payload OutfitDayPalettesPayload
		if err := json.Unmarshal(data, &payload); err != nil {
			return
		}
		parsed := parseDayPalettesPayload(payload.DayPalettes)
		if len(parsed) == 0 {
			return
		}
		outfitPaletteStoreMu.Lock()
		outfitPaletteStore = parsed
		outfitPaletteStoreMu.Unlock()
	})
}

func defaultOutfitDayPalettes() map[int][]string {
	return map[int][]string{
		1: {"#FF0000", "#CC0000", "#FF3333", "#990000", "#990000"},
		2: {"#FFFFFF", "#FFFFCC", "#FFFF99", "#FFFFC2", "#FFFFC2"},
		3: {"#FF00FF", "#FF66FF", "#CC00CC", "#FF007F", "#FF007F"},
		4: {"#009900", "#006600", "#00CC00", "#336600", "#336600"},
		5: {"#FFFF00", "#FF8000", "#FFFF33", "#CC6600", "#CC6600"},
		6: {"#00FFFF", "#0080FF", "#0000FF", "#66B2FF", "#66B2FF"},
		7: {"#000000", "#6600CC", "#9933FF", "#4C0099", "#4C0099"},
		8: {"#663300", "#808080", "#C0C0C0", "#E0E0E0", "#E0E0E0"},
	}
}

func normalizeHexColor(value string) string {
	text := strings.ToUpper(strings.TrimSpace(value))
	if text == "" {
		return "#FFFFFF"
	}
	if !strings.HasPrefix(text, "#") {
		text = "#" + text
	}
	if len(text) != 7 {
		return "#FFFFFF"
	}
	for _, ch := range text[1:] {
		if (ch < '0' || ch > '9') && (ch < 'A' || ch > 'F') {
			return "#FFFFFF"
		}
	}
	return text
}

func normalizeFiveShades(shades []string) []string {
	out := make([]string, 0, 5)
	for _, shade := range shades {
		trimmed := strings.TrimSpace(shade)
		if trimmed == "" {
			continue
		}
		out = append(out, normalizeHexColor(trimmed))
	}
	for len(out) < 5 {
		if len(out) == 0 {
			out = append(out, "#FFFFFF")
			continue
		}
		out = append(out, out[len(out)-1])
	}
	if len(out) > 5 {
		out = out[:5]
	}
	return out
}

func parseDayPalettesPayload(input map[string][]string) map[int][]string {
	out := map[int][]string{}
	for key, shades := range input {
		dayNumber := 0
		for _, ch := range key {
			if ch < '0' || ch > '9' {
				dayNumber = 0
				break
			}
			dayNumber = (dayNumber * 10) + int(ch-'0')
		}
		if dayNumber < 1 || dayNumber > 8 {
			continue
		}
		out[dayNumber] = normalizeFiveShades(shades)
	}
	return out
}

func serializeDayPalettes(input map[int][]string) map[string][]string {
	keys := make([]int, 0, len(input))
	for k := range input {
		keys = append(keys, k)
	}
	sort.Ints(keys)
	fixed := map[string][]string{}
	for _, key := range keys {
		fixed[itoa(key)] = normalizeFiveShades(input[key])
	}
	return fixed
}

func saveOutfitPaletteStore() {
	outfitPaletteStoreMu.RLock()
	payload := OutfitDayPalettesPayload{
		DayPalettes: serializeDayPalettes(outfitPaletteStore),
	}
	outfitPaletteStoreMu.RUnlock()
	data, err := json.MarshalIndent(payload, "", "  ")
	if err != nil {
		return
	}
	_ = os.WriteFile(outfitPaletteStoreFile, data, 0644)
}

func GetOutfitDayPalettes() map[int][]string {
	ensureOutfitPaletteStoreLoaded()
	outfitPaletteStoreMu.RLock()
	defer outfitPaletteStoreMu.RUnlock()
	out := map[int][]string{}
	for dayNumber, shades := range outfitPaletteStore {
		out[dayNumber] = append([]string{}, normalizeFiveShades(shades)...)
	}
	return out
}

func SetOutfitDayPalettes(dayPalettes map[int][]string) map[int][]string {
	ensureOutfitPaletteStoreLoaded()
	normalized := defaultOutfitDayPalettes()
	for dayNumber, shades := range dayPalettes {
		if dayNumber < 1 || dayNumber > 8 {
			continue
		}
		normalized[dayNumber] = normalizeFiveShades(shades)
	}
	outfitPaletteStoreMu.Lock()
	outfitPaletteStore = normalized
	outfitPaletteStoreMu.Unlock()
	saveOutfitPaletteStore()
	return GetOutfitDayPalettes()
}

func SetSingleOutfitDayPalette(dayNumber int, shades []string) map[int][]string {
	if dayNumber < 1 || dayNumber > 8 {
		return GetOutfitDayPalettes()
	}
	ensureOutfitPaletteStoreLoaded()
	outfitPaletteStoreMu.Lock()
	outfitPaletteStore[dayNumber] = normalizeFiveShades(shades)
	outfitPaletteStoreMu.Unlock()
	saveOutfitPaletteStore()
	return GetOutfitDayPalettes()
}

func OutfitPaletteForDay(dayNumber int) []string {
	ensureOutfitPaletteStoreLoaded()
	outfitPaletteStoreMu.RLock()
	defer outfitPaletteStoreMu.RUnlock()
	shades, ok := outfitPaletteStore[dayNumber]
	if !ok {
		defaults := defaultOutfitDayPalettes()
		return defaults[dayNumber]
	}
	return append([]string{}, normalizeFiveShades(shades)...)
}

func itoa(n int) string {
	if n == 0 {
		return "0"
	}
	digits := []byte{}
	for n > 0 {
		digits = append([]byte{byte('0' + (n % 10))}, digits...)
		n /= 10
	}
	return string(digits)
}
