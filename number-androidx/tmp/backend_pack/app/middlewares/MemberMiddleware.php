<?php

namespace App\Middleware;
class MemberMiddleware{
    public function __invoke($request, $response, $next)
    {
        if (!isset($_SESSION[PREFIX.'uid'])){
            return $response->withRedirect(PATH.'/login');
        }
        $response = $next($request, $response);
        return $response;
    }
}