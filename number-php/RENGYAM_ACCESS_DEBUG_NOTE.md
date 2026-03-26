# RengYam Access Debug Note

## Summary

This note records the long-running fix for the `ฤกษ์ยาม` unlock/restore flow so the same mistake does not happen again.

## Real Root Cause

The main confusion was not Android logic first. The real issue was:

1. The live website `numberniceic.online` is served from:
   - `/var/www/html/app/routes.php`
   - `/var/www/html/app/Managers/UserController.php`
2. Earlier fixes were deployed to other server paths such as:
   - `/home/tayap/ananya-php/app/...`
   - `/home/tayap/routes.php`
3. Because of that, API responses from the live site did not change even though code in other folders had been updated.

## Live Server Routing

Nginx config for `numberniceic.online` proxies to:

- `http://127.0.0.1:81`

The active PHP codebase for this API is under:

- `/var/www/html`

Important live files:

- `/var/www/html/index.php`
- `/var/www/html/app/routes.php`
- `/var/www/html/app/Managers/UserController.php`

## Backend Logic Required

To restore access correctly after login:

1. `POST /member/vipcode`
   - must insert a row into `memberuse`
   - fields used: `viptype`, `codename`, `memberid`, `dateadd`
2. `POST /member/login`
   - must return `rengyam_access`
3. `GET /member/info/{memberid}`
   - must return `rengyam_access`

Expected payload shape:

```json
{
  "rengyam_access": {
    "granted": true,
    "viptype": "rengyam_yearly",
    "codename": "example_code",
    "dateadd": "2026-March-26",
    "expire_at": "2027-03-01",
    "expired": false
  }
}
```

## Database Facts

Actual live table schema for `memberuse`:

- primary key column is `memuseid`
- not `id`

Columns:

- `memuseid`
- `viptype`
- `codename`
- `memberid`
- `dateadd`

When ordering latest entitlement row, use:

```sql
ORDER BY memuseid DESC
```

Do not use:

```sql
ORDER BY id DESC
```

## Recovery Rule For Old Data

Some old records may not have the right `viptype` in `memberuse`.
To support old data safely, backend should also inspect `secretcode.codetype` via `codename`.

Recommended query idea:

```sql
SELECT mu.viptype, mu.codename, mu.dateadd, sc.codetype AS secret_codetype
FROM memberuse mu
LEFT JOIN secretcode sc ON sc.codename = mu.codename
WHERE mu.memberid = :mid
  AND (
    mu.viptype IN ('rengyam_yearly', 'rengyam_vip')
    OR sc.codetype IN ('rengyam_yearly', 'rengyam_vip')
  )
ORDER BY mu.memuseid DESC
LIMIT 1
```

## Android Rule

Android should not rely on login state alone.

Correct flow:

1. backend returns `rengyam_access`
2. Android stores access per `userId`
3. page checks this entitlement before allowing category tap

Files already updated in Android:

- `/Users/tayap/Numberniceic-Super-Apps/number-androidx/app/src/main/java/com/numberniceic/ui/renkyam/RengYamF.kt`
- `/Users/tayap/Numberniceic-Super-Apps/number-androidx/app/src/main/java/com/numberniceic/ui/auth/UserLoginF.kt`
- `/Users/tayap/Numberniceic-Super-Apps/number-androidx/app/src/main/java/com/numberniceic/ui/auth/UserLogoutF.kt`
- `/Users/tayap/Numberniceic-Super-Apps/number-androidx/app/src/main/java/com/numberniceic/ui/AppActivity.kt`
- `/Users/tayap/Numberniceic-Super-Apps/number-androidx/app/src/main/java/com/numberniceic/utils/RengyamAccessManager.kt`

## Proven Debug Checklist

When this problem appears again, check in this order:

1. Confirm the member's real `memberid` from `POST /member/login`
2. Query `memberuse` for that `memberid`
3. Confirm the row actually exists
4. Confirm live API returns `rengyam_access`
5. Confirm the server path being edited is really `/var/www/html/app/...`
6. Confirm Android stores returned entitlement after login/refresh

## Important Lesson

If backend does not return `rengyam_access`, Android will appear broken even when the local logic is correct.

For this feature, backend truth must be verified first.
