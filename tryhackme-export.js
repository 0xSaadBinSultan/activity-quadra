/* Read only the public yearly graph currently rendered in your browser. */
(() => {
  if (location.hostname !== 'tryhackme.com' || !location.pathname.startsWith('/p/')) {
    alert('Open your public TryHackMe profile first.'); return;
  }
  const grid = document.querySelector('[data-testid="yearly-activity-grid"]');
  const year = Number(document.querySelector('[data-testid="year-value"]')?.textContent);
  if (!grid || !year) { alert('Wait for the yearly activity graph to load.'); return; }
  const data = {
    provider: 'tryhackme', username: decodeURIComponent(location.pathname.split('/').filter(Boolean).at(-1)),
    year, total: Number(grid.closest('section').querySelector('strong').textContent),
    colors: Array.from(grid.querySelectorAll('span[class*="StyledDaySquare"]')).map(cell => getComputedStyle(cell).backgroundColor),
    captured: new Date().toISOString(), source: 'public profile browser snapshot'
  };
  const blob = new Blob([JSON.stringify(data, null, 2)], {type: 'application/json'});
  const url = URL.createObjectURL(blob); const link = document.createElement('a');
  link.href = url; link.download = 'tryhackme-activity-' + year + '.json'; link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
})();
