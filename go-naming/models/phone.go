package models

type PhoneSell struct {
	ID          int    `json:"pnumber_id"`
	Position    int    `json:"pnumber_position"`
	Number      string `json:"pnumber_num"`
	Sum         string `json:"pnumber_sum"`
	Price       int    `json:"pnumber_price"`
	PhoneGroup  string `json:"phone_group"`
	SellStatus  string `json:"sell_status"`
	PrefixGroup string `json:"prefix_group"`
}
