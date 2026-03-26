<?php
namespace App\Managers;

use Psr\Container\ContainerInterface;

class Manager
{
    protected $container;

    function __construct(ContainerInterface $container)
    {
        $this->container = $container;
    }
    public function __get($property)
    {
        if ($this->container->has($property)) {
            return $this->container->get($property);
        }
        // Fallback for Slim 3 style if needed, or return null
        return null;
    }
}
