<?php
namespace App\Managers\Telephone;

use App\Daos\PhoneNumItemCollectionDao;
use App\Daos\PhoneNumItemDao;
use App\Managers\Manager;

class PhoneNumberSellManager extends Manager
{

    private $apiPhoneSell;

    public function mainPhoneSell($request, $response, $args)
    {

        $this->apiPhoneSell = new PhoneNumItemCollectionDao();

        /*$this->apiPhoneSell->phonenumberSell = array(new PhoneNumItemDao('00', '00','085999999','44', 25000, 'vip', 'online', '095'));*/


        $sql = "SELECT * FROM phonenumber_sell where phonenumber_sell.sell_status NOT LIKE 'f' ORDER BY pnumber_price DESC";
        $result = $this->db->prepare($sql);
        $result->execute();
        $data = $result->fetchAll(\PDO::FETCH_OBJ);

        $this->apiPhoneSell->phonenumberSell = $data;


        $sql = "SELECT * FROM phonenumber_sell WHERE phonenumber_sell.phone_group = 'viptop4' && phonenumber_sell.sell_status NOT LIKE 'f' ORDER BY pnumber_position ASC LIMIT 4";
        $result = $this->db->prepare($sql);
        $result->execute();
        $data = $result->fetchAll(\PDO::FETCH_OBJ);

        $this->apiPhoneSell->phonenumTop4 = $data;



        $response->getBody()->write(json_encode($this->apiPhoneSell));
        return $response->withHeader('Content-Type', 'application/json');

    }

}