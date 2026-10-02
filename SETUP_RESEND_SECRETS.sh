#!/usr/bin/env bash
set -e
printf 'Masukkan API Key Resend (tidak disimpan di file):\n'
firebase functions:secrets:set RESEND_API_KEY
printf '\nMasukkan RESEND_FROM, contoh: MMC PONSEL <noreply@domainanda.com>\n'
firebase functions:secrets:set RESEND_FROM
printf '\nDeploy Cloud Functions? [y/N] '
read ans
if [[ "$ans" == "y" || "$ans" == "Y" ]]; then
  firebase deploy --only functions
fi
