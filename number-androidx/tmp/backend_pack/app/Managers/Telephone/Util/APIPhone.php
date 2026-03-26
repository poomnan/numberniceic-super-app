<?php
namespace App\Managers\Telephone\Util;

class APIPhone {
    public $xmessage;
    public $scoreTotalOfTotal = array();

    public $percentAfterDupAB = 0;
    public $percentAfterStickNum = array();

    public $percentTotalOfTotal = array();

    public $miPercentScoreNum = array();

    public $pairSpecialX = array();
    public $scoreDupMi = 0;
    public $countPairZero = 0;
    public $pairDup = array();

    public $specialGoal = array();

    public $miracleSummary = array();

    public $pairsA, $pairsB = array();
    public $pairSum;
    public $pairsUnique = array();

    public $scoreByPairsCount;
    public $scoreByContinueCountPairsA;
    public $scoreByContinueCountPairsB;


    public $scorePairByPositionA = array();
    public $scorePairByPositionB = array();
    public $scorePairBySum = array();

    public $dataSortByType = array();


}