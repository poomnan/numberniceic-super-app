<?php
namespace App\Daos;

class PairItemCollectionDao{

    public $pairsItemMiracleDao = array();

    /**
     * PairItemCollectionDao constructor.
     * @param array $pairsItemMiracle
     */
    public function __construct(array $pairsItemMiracle)
    {
        $this->pairsItemMiracleDao = $pairsItemMiracle;
    }


}