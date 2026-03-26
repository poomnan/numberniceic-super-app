package main

import (
	"database/sql"
	"fmt"

	_ "github.com/go-sql-driver/mysql"
)

func main() {
	db, err := sql.Open("mysql", "zoqlszwh_ananyadb:IntelliP24.X@tcp(43.228.85.200:3306)/zoqlszwh_ananyadb?parseTime=true")
	if err != nil {
		fmt.Println(err)
		return
	}
	defer db.Close()

	rows, err := db.Query("SELECT id, memberid, assignment_type, buddha_id, assigned_at FROM user_buddha_assign ORDER BY assigned_at DESC LIMIT 5")
	if err != nil {
		fmt.Println(err)
		return
	}
	defer rows.Close()

	for rows.Next() {
		var id, memberid, buddha_id int
		var assign_type string
		var assigned_at sql.NullTime
		err := rows.Scan(&id, &memberid, &assign_type, &buddha_id, &assigned_at)
		if err != nil {
			fmt.Println(err)
			continue
		}
		fmt.Printf("id=%d memberid=%d type=%s buddha_id=%d assigned_at=%v\n", id, memberid, assign_type, buddha_id, assigned_at.Time)
	}
}
