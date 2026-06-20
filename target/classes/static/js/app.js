// Common Theme & Helper Functions
function initTheme() {
    const themeBtn = document.getElementById('theme-toggle');
    if (!themeBtn) return;

    // Check saved theme or preference
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'dark' || (!savedTheme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
        document.body.classList.add('dark-theme');
        themeBtn.innerHTML = '☀️ 라이트 모드';
    } else {
        document.body.classList.remove('dark-theme');
        themeBtn.innerHTML = '🌙 다크 모드';
    }

    themeBtn.addEventListener('click', () => {
        document.body.classList.toggle('dark-theme');
        const isDark = document.body.classList.contains('dark-theme');
        localStorage.setItem('theme', isDark ? 'dark' : 'light');
        themeBtn.innerHTML = isDark ? '☀️ 라이트 모드' : '🌙 다크 모드';
    });
}

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('active');
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('active');
    }
}

// Global Validation Helper (Realtime warnings & Price calculations)
let validationTimeout = null;
function handleRealtimeValidation(inputEl, typeEl, warningBoxId, pricePreviewId) {
    const menuNames = inputEl.value;
    const mealType = typeEl.value;

    if (validationTimeout) {
        clearTimeout(validationTimeout);
    }

    // Debounce to reduce server queries
    validationTimeout = setTimeout(async () => {
        if (!menuNames.trim()) {
            document.getElementById(warningBoxId).style.display = 'none';
            document.getElementById(pricePreviewId).innerText = '0원';
            return;
        }
        try {
            const res = await fetch(`/api/schedules/validate?menuNames=${encodeURIComponent(menuNames)}&mealType=${mealType}`);
            if (res.ok) {
                const data = await res.json();
                const warnBox = document.getElementById(warningBoxId);
                const warnUl = warnBox.querySelector('ul');
                warnUl.innerHTML = '';

                if (data.warnings && data.warnings.length > 0) {
                    data.warnings.forEach(warn => {
                        const li = document.createElement('li');
                        li.innerText = warn;
                        warnUl.appendChild(li);
                    });
                    warnBox.style.display = 'block';
                } else {
                    warnBox.style.display = 'none';
                }

                document.getElementById(pricePreviewId).innerText = data.totalPrice.toLocaleString() + '원';
            }
        } catch (err) {
            console.error('Validation error:', err);
        }
    }, 300);
}

// ==========================================
// 1. Calendar Management
// ==========================================
let currentYear, currentMonth;
let schedulesData = [];

function initCalendar() {
    const today = new Date();
    currentYear = today.getFullYear();
    currentMonth = today.getMonth(); // 0-based

    renderCalendarHeader();
    fetchAndRenderSchedules();

    // Navigation buttons
    document.getElementById('prev-month-btn').addEventListener('click', () => {
        currentMonth--;
        if (currentMonth < 0) {
            currentMonth = 11;
            currentYear--;
        }
        renderCalendarHeader();
        fetchAndRenderSchedules();
    });

    document.getElementById('next-month-btn').addEventListener('click', () => {
        currentMonth++;
        if (currentMonth > 11) {
            currentMonth = 0;
            currentYear++;
        }
        renderCalendarHeader();
        fetchAndRenderSchedules();
    });

    // Modal forms bindings
    const scheduleInput = document.getElementById('schedule-menu-names');
    const scheduleType = document.getElementById('schedule-meal-type');
    if (scheduleInput && scheduleType) {
        const handler = () => handleRealtimeValidation(scheduleInput, scheduleType, 'schedule-warnings', 'schedule-price-preview');
        scheduleInput.addEventListener('input', handler);
        scheduleType.addEventListener('change', handler);
    }

    // Modal submission
    document.getElementById('schedule-form').addEventListener('submit', saveSchedule);
}

function renderCalendarHeader() {
    document.getElementById('calendar-title').innerText = `${currentYear}년 ${currentMonth + 1}월`;
}

async function fetchAndRenderSchedules() {
    // Get first day and last day of current calendar grid (including previous/next month days)
    const firstDayOfMonth = new Date(currentYear, currentMonth, 1);
    const startOffset = firstDayOfMonth.getDay(); // 0 is Sunday
    const startDate = new Date(currentYear, currentMonth, 1 - startOffset);
    
    // We render 6 weeks (42 days)
    const endDate = new Date(startDate.getTime());
    endDate.setDate(endDate.getDate() + 41);

    const startStr = formatDateISO(startDate);
    const endStr = formatDateISO(endDate);

    try {
        const res = await fetch(`/api/schedules?startDate=${startStr}&endDate=${endStr}`);
        if (res.ok) {
            schedulesData = await res.json();
        } else {
            schedulesData = [];
        }
    } catch (err) {
        console.error('Failed to load schedules', err);
        schedulesData = [];
    }
    buildCalendarGrid(startDate);
}

function buildCalendarGrid(startDate) {
    const grid = document.getElementById('calendar-grid');
    // Clear previous dynamic cells (but keep the 7 headers if we hardcoded them, or clear all and append)
    // Assume we keep standard structure: clear all day cells
    const headerDays = Array.from(grid.querySelectorAll('.calendar-header-day'));
    grid.innerHTML = '';
    headerDays.forEach(h => grid.appendChild(h));

    const tempDate = new Date(startDate.getTime());
    
    for (intCell = 0; intCell < 42; intCell++) {
        const dateStr = formatDateISO(tempDate);
        const dayNum = tempDate.getDate();
        const isCurrentMonth = tempDate.getMonth() === currentMonth;
        
        // CSS classes
        let cellClass = 'calendar-day';
        if (!isCurrentMonth) cellClass += ' other-month';
        if (tempDate.getDay() === 0) cellClass += ' sunday';
        if (tempDate.getDay() === 6) cellClass += ' saturday';

        const cell = document.createElement('div');
        cell.className = cellClass;
        cell.dataset.date = dateStr;

        const dayNumEl = document.createElement('div');
        dayNumEl.className = 'day-number';
        dayNumEl.innerText = dayNum;
        cell.appendChild(dayNumEl);

        // Filter schedules for this date
        const daySchedules = schedulesData.filter(s => s.mealDate === dateStr);
        // Sort by mealType order: MORNING, AM_SNACK, LUNCH, PM_SNACK, DINNER
        const order = ['MORNING', 'AM_SNACK', 'LUNCH', 'PM_SNACK', 'DINNER'];
        daySchedules.sort((a, b) => order.indexOf(a.mealType) - order.indexOf(b.mealType));

        const badgesContainer = document.createElement('div');
        badgesContainer.className = 'day-meals';

        daySchedules.forEach(sched => {
            const badge = document.createElement('div');
            let badgeClass = 'meal-badge';
            if (sched.mealType === 'MORNING') badgeClass += ' morning';
            else if (sched.mealType === 'LUNCH') badgeClass += ' lunch';
            else if (sched.mealType === 'DINNER') badgeClass += ' dinner';
            else badgeClass += ' snack';

            badge.className = badgeClass;
            const typeLabel = getMealTypeLabel(sched.mealType);
            const dietLabel = getDietTypeLabel(sched.dietType);
            badge.innerText = `[${typeLabel}/${dietLabel}] ${sched.menuNames}`;
            badge.title = `${sched.menuNames} (${sched.totalPrice.toLocaleString()}원) - ${sched.memo || ''}`;
            
            badge.addEventListener('click', (e) => {
                e.stopPropagation(); // Cell click prevent
                openEditScheduleModal(sched);
            });
            badgesContainer.appendChild(badge);
        });

        cell.appendChild(badgesContainer);

        // Click to add schedule
        cell.addEventListener('click', () => {
            openAddScheduleModal(dateStr);
        });

        grid.appendChild(cell);
        tempDate.setDate(tempDate.getDate() + 1);
    }
}

function openAddScheduleModal(dateStr) {
    document.getElementById('schedule-modal-title').innerText = `${dateStr} 식단 등록`;
    document.getElementById('schedule-id').value = '';
    document.getElementById('schedule-date').value = dateStr;
    document.getElementById('schedule-meal-type').value = 'MORNING';
    document.getElementById('schedule-diet-type').value = 'NORMAL';
    document.getElementById('schedule-menu-names').value = '';
    document.getElementById('schedule-memo').value = '';
    document.getElementById('schedule-warnings').style.display = 'none';
    document.getElementById('schedule-price-preview').innerText = '0원';
    document.getElementById('schedule-delete-btn').style.display = 'none';
    openModal('schedule-modal');
}

function openEditScheduleModal(sched) {
    document.getElementById('schedule-modal-title').innerText = `${sched.mealDate} 식단 수정`;
    document.getElementById('schedule-id').value = sched.id;
    document.getElementById('schedule-date').value = sched.mealDate;
    document.getElementById('schedule-meal-type').value = sched.mealType;
    document.getElementById('schedule-diet-type').value = sched.dietType;
    document.getElementById('schedule-menu-names').value = sched.menuNames;
    document.getElementById('schedule-memo').value = sched.memo || '';
    
    // Trigger price preview & validation
    handleRealtimeValidation(
        document.getElementById('schedule-menu-names'),
        document.getElementById('schedule-meal-type'),
        'schedule-warnings',
        'schedule-price-preview'
    );
    
    document.getElementById('schedule-delete-btn').style.display = 'inline-flex';
    openModal('schedule-modal');
}

async function saveSchedule(e) {
    e.preventDefault();
    const id = document.getElementById('schedule-id').value;
    const data = {
        id: id ? parseInt(id) : null,
        mealDate: document.getElementById('schedule-date').value,
        mealType: document.getElementById('schedule-meal-type').value,
        dietType: document.getElementById('schedule-diet-type').value,
        menuNames: document.getElementById('schedule-menu-names').value,
        memo: document.getElementById('schedule-memo').value
    };

    try {
        const res = await fetch('/api/schedules', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (res.ok) {
            closeModal('schedule-modal');
            fetchAndRenderSchedules();
        } else {
            alert('식단을 저장하는 데 실패했습니다.');
        }
    } catch (err) {
        console.error(err);
    }
}

async function deleteSchedule() {
    const id = document.getElementById('schedule-id').value;
    if (!id) return;
    if (!confirm('정말 이 식단을 삭제하시겠습니까?')) return;

    try {
        const res = await fetch(`/api/schedules/${id}`, { method: 'DELETE' });
        if (res.ok) {
            closeModal('schedule-modal');
            fetchAndRenderSchedules();
        } else {
            alert('삭제에 실패했습니다.');
        }
    } catch (err) {
        console.error(err);
    }
}

async function clearSchedules() {
    if (!confirm('정말 달력의 모든 식단 일정을 초기화하시겠습니까?')) {
        return;
    }
    if (!confirm('이 작업은 되돌릴 수 없습니다. 정말로 모든 일정을 영구적으로 삭제하시겠습니까?')) {
        return;
    }

    try {
        const res = await fetch('/api/schedules/clear', { method: 'DELETE' });
        if (res.ok) {
            alert('모든 식단 일정이 성공적으로 초기화되었습니다.');
            fetchAndRenderSchedules();
        } else {
            alert('일정 초기화에 실패했습니다.');
        }
    } catch (err) {
        console.error(err);
    }
}

// Deployment Action
async function deployPattern() {
    const startDate = document.getElementById('deploy-start-date').value;
    const startPatternDay = document.getElementById('deploy-pattern-day').value;

    if (!startDate) {
        alert('시작 날짜를 선택해주세요.');
        return;
    }

    try {
        const res = await fetch(`/api/schedules/deploy?startDate=${startDate}&startPatternDay=${startPatternDay}`, {
            method: 'POST'
        });
        if (res.ok) {
            const data = await res.json();
            alert(data.message);
            closeModal('deploy-modal');
            fetchAndRenderSchedules();
        } else {
            alert('배포 중 에러가 발생했습니다.');
        }
    } catch (err) {
        console.error(err);
    }
}


// ==========================================
// 2. Menu Items Management
// ==========================================
function initMenuManagement() {
    // Save or update
    const form = document.getElementById('menu-form');
    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const id = document.getElementById('menu-id').value;
            const data = {
                id: id ? parseInt(id) : null,
                name: document.getElementById('menu-name').value,
                category: document.getElementById('menu-category').value,
                unitPrice: parseInt(document.getElementById('menu-price').value),
                isSpicy: document.getElementById('menu-spicy').checked,
                isVegetable: document.getElementById('menu-vegetable').checked
            };

            const res = await fetch('/api/menus', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (res.ok) {
                location.reload();
            } else {
                alert('메뉴 저장에 실패했습니다.');
            }
        });
    }
}

function openAddMenuModal() {
    document.getElementById('menu-modal-title').innerText = '신규 식자재/메뉴 등록';
    document.getElementById('menu-id').value = '';
    document.getElementById('menu-name').value = '';
    document.getElementById('menu-category').value = '반찬';
    document.getElementById('menu-price').value = 1000;
    document.getElementById('menu-spicy').checked = false;
    document.getElementById('menu-vegetable').checked = true;
    openModal('menu-modal');
}

function openEditMenuModal(id, name, category, price, isSpicy, isVegetable) {
    document.getElementById('menu-modal-title').innerText = '식자재/메뉴 상세 및 수정';
    document.getElementById('menu-id').value = id;
    document.getElementById('menu-name').value = name;
    document.getElementById('menu-category').value = category;
    document.getElementById('menu-price').value = price;
    document.getElementById('menu-spicy').checked = isSpicy;
    document.getElementById('menu-vegetable').checked = isVegetable;
    openModal('menu-modal');
}

async function deleteMenu(id) {
    if (!confirm('정말 이 메뉴를 삭제하시겠습니까? (식단 가격 합산 계산에서 제외될 수 있습니다)')) return;
    const res = await fetch(`/api/menus/${id}`, { method: 'DELETE' });
    if (res.ok) {
        location.reload();
    } else {
        alert('삭제에 실패했습니다.');
    }
}


// ==========================================
// 3. Pattern Management
// ==========================================
function initPatternManagement() {
    const patternInput = document.getElementById('pattern-menu-names');
    const patternType = document.getElementById('pattern-meal-type');
    
    if (patternInput && patternType) {
        const handler = () => handleRealtimeValidation(patternInput, patternType, 'pattern-warnings', 'pattern-price-preview');
        patternInput.addEventListener('input', handler);
        patternType.addEventListener('change', handler);
    }

    document.getElementById('pattern-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('pattern-id').value;
        const data = {
            id: id ? parseInt(id) : null,
            patternDay: parseInt(document.getElementById('pattern-day').value),
            mealType: document.getElementById('pattern-meal-type').value,
            dietType: document.getElementById('pattern-diet-type').value,
            menuNames: document.getElementById('pattern-menu-names').value
        };

        const res = await fetch('/api/patterns', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (res.ok) {
            location.reload();
        } else {
            alert('패턴 저장에 실패했습니다.');
        }
    });
}

function openAddPatternModal() {
    document.getElementById('pattern-modal-title').innerText = '신규 식단 패턴 등록';
    document.getElementById('pattern-id').value = '';
    document.getElementById('pattern-day').value = 1;
    document.getElementById('pattern-meal-type').value = 'MORNING';
    document.getElementById('pattern-diet-type').value = 'NORMAL';
    document.getElementById('pattern-menu-names').value = '';
    document.getElementById('pattern-warnings').style.display = 'none';
    document.getElementById('pattern-price-preview').innerText = '0원';
    openModal('pattern-modal');
}

function openEditPatternModal(id, day, mealType, dietType, menuNames) {
    document.getElementById('pattern-modal-title').innerText = '식단 패턴 수정';
    document.getElementById('pattern-id').value = id;
    document.getElementById('pattern-day').value = day;
    document.getElementById('pattern-meal-type').value = mealType;
    document.getElementById('pattern-diet-type').value = dietType;
    document.getElementById('pattern-menu-names').value = menuNames;

    handleRealtimeValidation(
        document.getElementById('pattern-menu-names'),
        document.getElementById('pattern-meal-type'),
        'pattern-warnings',
        'pattern-price-preview'
    );

    openModal('pattern-modal');
}

async function deletePattern(id) {
    if (!confirm('정말 이 패턴을 삭제하시겠습니까?')) return;
    const res = await fetch(`/api/patterns/${id}`, { method: 'DELETE' });
    if (res.ok) {
        location.reload();
    } else {
        alert('삭제에 실패했습니다.');
    }
}


// ==========================================
// Utilites
// ==========================================
function formatDateISO(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function getMealTypeLabel(type) {
    const map = {
        'MORNING': '아침',
        'AM_SNACK': '오전간식',
        'LUNCH': '점심',
        'PM_SNACK': '오후간식',
        'DINNER': '저녁'
    };
    return map[type] || type;
}

function getDietTypeLabel(type) {
    const map = {
        'NORMAL': '일반',
        'DIABETES': '당뇨',
        'CHOPPED': '다진',
        'GROUND': '갈식',
        'TOFU': '연두부',
        'LIQUID_RICE': '미음',
        'PORRIDGE': '죽식'
    };
    return map[type] || type;
}

document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    if (document.getElementById('calendar-grid')) {
        initCalendar();
    }
    if (document.getElementById('menu-form')) {
        initMenuManagement();
    }
    if (document.getElementById('pattern-form')) {
        initPatternManagement();
    }
});

