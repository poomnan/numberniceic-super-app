<?php
namespace App\Daos;

class PhoneNumItemDao {
    public $pnumberId;
    public $pnumberPosition;
    public $pnumberNum;
    public $pnumberSum;
    public $pnumberPize;
    public $phoneGroup;
    public $sellStatus;
    public $prefixGroup;

    /**
     * PhoneNumItemDao constructor.
     * @param $pnumberId
     * @param $pnumberPosition
     * @param $pnumberNum
     * @param $pnumberSum
     * @param $pnumberPize
     * @param $phoneGroup
     * @param $sellStatus
     * @param $prefixGroup
     */
    public function
    __construct($pnumberId, $pnumberPosition, $pnumberNum, $pnumberSum, $pnumberPize, $phoneGroup, $sellStatus, $prefixGroup)
    {
        $this->pnumberId = $pnumberId;
        $this->pnumberPosition = $pnumberPosition;
        $this->pnumberNum = $pnumberNum;
        $this->pnumberSum = $pnumberSum;
        $this->pnumberPize = $pnumberPize;
        $this->phoneGroup = $phoneGroup;
        $this->sellStatus = $sellStatus;
        $this->prefixGroup = $prefixGroup;
    }



}
