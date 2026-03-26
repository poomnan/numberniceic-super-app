<?php
require 'vendor/autoload.php';
var_dump(class_exists(\App\Managers\AdminController::class));
require_once 'app/Managers/AdminController.php';
var_dump(class_exists(\App\Managers\AdminController::class));
var_dump(method_exists(\App\Managers\AdminController::class, 'addRealName'));
