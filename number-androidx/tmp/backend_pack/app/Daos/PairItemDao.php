<?php
namespace App\Daos;

class PairItemDao {
    public $number;
    public $description;
    public $percentile;
    public $type;
    public $point;
    public $detail;


    public function __construct($number, $description, $percentile, $type, $point, $detail)
    {
        $this->number = $number;
        $this->description = $description;
        $this->percentile = $percentile;
        $this->type = $type;
        $this->point = $point;
        $this->detail = $detail;
    }




}
