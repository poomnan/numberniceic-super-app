package astro

// Cell represents a cell in the 3x3 matrix
type Cell struct {
	Position int    `json:"position"`
	Day      string `json:"day"`
}

// Matrix represents a 2D array of cells
type Matrix [][]Cell

// KalagniResult represents the result of kalagni calculation
type KalagniResult struct {
	Day      string `json:"day"`
	Position int    `json:"position"`
	BirthDay string `json:"birth_day,omitempty"`
	Age      int    `json:"age,omitempty"`
}

// FooDay represents a Foo day result
type FooDay struct {
	Date        string `json:"date"`
	ThaiDate    string `json:"thai_date"`
	Description string `json:"description"`
	IsFoo       bool   `json:"is_foo"`
}

// CalendarResponse represents calendar API response
type CalendarResponse struct {
	Year           int            `json:"year"`
	FooDays        []FooDay       `json:"foo_days,omitempty"`
	Kalagni        *KalagniResult `json:"kalagni,omitempty"`
	SittiChok      []string       `json:"sitti_chok,omitempty"`
	UbathDays      []string       `json:"ubath_days,omitempty"`
	LokawinatDays  []string       `json:"lokawinat_days,omitempty"`
}