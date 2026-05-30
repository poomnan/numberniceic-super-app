package services

import (
	"context"
	"database/sql"
	"database/sql/driver"
	"io"
	"sync"
	"testing"
)

// Define a thread-safe mock driver to mock queries to the names database
type mockDriver struct {
	mu    sync.Mutex
	names map[string]bool
}

func (d *mockDriver) Open(name string) (driver.Conn, error) {
	d.mu.Lock()
	defer d.mu.Unlock()
	return &mockConn{names: d.names}, nil
}

type mockConn struct {
	names map[string]bool
}

func (c *mockConn) Prepare(query string) (driver.Stmt, error) {
	return &mockStmt{names: c.names}, nil
}

func (c *mockConn) Close() error              { return nil }
func (c *mockConn) Begin() (driver.Tx, error) { return nil, nil }

type mockStmt struct {
	names map[string]bool
}

func (s *mockStmt) Close() error                                    { return nil }
func (s *mockStmt) NumInput() int                                   { return 1 }
func (s *mockStmt) Exec(args []driver.Value) (driver.Result, error) { return nil, nil }
func (s *mockStmt) Query(args []driver.Value) (driver.Rows, error) {
	if len(args) == 0 {
		return &mockRows{exists: false}, nil
	}
	name, ok := args[0].(string)
	if !ok {
		return &mockRows{exists: false}, nil
	}
	exists := s.names[name]
	return &mockRows{exists: exists, read: false}, nil
}

type mockRows struct {
	exists bool
	read   bool
}

func (r *mockRows) Columns() []string {
	return []string{"constant"}
}

func (r *mockRows) Close() error {
	return nil
}

func (r *mockRows) Next(dest []driver.Value) error {
	if !r.exists || r.read {
		return io.EOF
	}
	r.read = true
	dest[0] = int64(1)
	return nil
}

var (
	registerOnce sync.Once
	mockDrv      = &mockDriver{
		names: map[string]bool{
			"สมชาย":  true,
			"ณัฐพล":  true,
			"สมศักดิ์": true,
		},
	}
)

func getMockDB(t *testing.T) *sql.DB {
	registerOnce.Do(func() {
		sql.Register("mock_name_router_driver", mockDrv)
	})
	db, err := sql.Open("mock_name_router_driver", "mock_dsn")
	if err != nil {
		t.Fatalf("failed to open mock db: %v", err)
	}
	return db
}

func TestRouteThaiInput(t *testing.T) {
	db := getMockDB(t)
	defer db.Close()

	tests := []struct {
		name           string
		input          string
		expectedType   NameType
		expectedReason string // substring to match in reason
	}{
		// Condition 1: Length and space limits
		{
			name:         "very long text exceeds 15 runes (meaning)",
			input:        "แสงสว่างแห่งดวงอาทิตย์ในยามเช้าอันอบอุ่น",
			expectedType: Meaning,
		},
		{
			name:         "long text with space (meaning)",
			input:        "สมชาย ยิ่งใหญ่เจริญสุขดีเลิศ",
			expectedType: Meaning,
		},
		{
			name:         "non-Thai characters (english)",
			input:        "Somchai",
			expectedType: Meaning,
		},
		{
			name:         "non-Thai characters (digits/punctuation)",
			input:        "สมชาย123",
			expectedType: Meaning,
		},

		// Condition 3: Explicit grammar or semantic verbs (meaning)
		{
			name:         "starts with ผู้มี (meaning)",
			input:        "ผู้มีบารมี",
			expectedType: Meaning,
		},
		{
			name:         "starts with อันเป็น (meaning)",
			input:        "อันเป็นที่รัก",
			expectedType: Meaning,
		},
		{
			name:         "contains แปลว่า (meaning)",
			input:        "สมชาย แปลว่า ผู้ชายที่มีความสมบูรณ์",
			expectedType: Meaning,
		},
		{
			name:         "space with connector (meaning)",
			input:        "ความรัก และ ความสุข",
			expectedType: Meaning,
		},

		// Condition 2: Database match
		{
			name:         "exact match in DB (first name)",
			input:        "สมชาย",
			expectedType: FirstName,
		},
		{
			name:         "exact match in DB with spaces trimmed (first name)",
			input:        "  ณัฐพล  ",
			expectedType: FirstName,
		},

		// Surname prefix/suffix logic (strong matches)
		{
			name:         "strong surname prefix ณ (last name)",
			input:        "ณ อยุธยา",
			expectedType: LastName,
		},
		{
			name:         "strong surname suffix วงศ์ (last name)",
			input:        "เลิศประเสริฐวงศ์",
			expectedType: LastName,
		},
		{
			name:         "strong surname suffix กุล (last name)",
			input:        "รักธรรมกุล",
			expectedType: LastName,
		},

		// Weak surname pattern vs First name prefix & Length heuristic
		{
			name:         "weak prefix with long length (last name)",
			input:        "วิจิตรปัญญา",
			expectedType: LastName,
		},
		{
			name:         "first name prefix with short length (first name)",
			input:        "กิตติ",
			expectedType: FirstName,
		},
		{
			name:         "first name prefix (first name)",
			input:        "ณัฐ",
			expectedType: FirstName,
		},

		// Heuristics and Fallbacks
		{
			name:         "long word not in DB (last name)",
			input:        "รุ่งประภามณฑล",
			expectedType: LastName,
		},
		{
			name:         "short single word fallback (first name)",
			input:        "เดชา",
			expectedType: FirstName,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			res, err := RouteThaiInput(context.Background(), db, tt.input)
			if err != nil {
				t.Fatalf("unexpected error: %v", err)
			}
			if res.Type != tt.expectedType {
				t.Errorf("RouteThaiInput(%q) Type = %s, want %s (Reason: %s)", tt.input, res.Type, tt.expectedType, res.Reason)
			}
			if res.Confidence <= 0.0 || res.Confidence > 1.0 {
				t.Errorf("RouteThaiInput(%q) Confidence = %f, want range (0.0, 1.0]", tt.input, res.Confidence)
			}
		})
	}
}
