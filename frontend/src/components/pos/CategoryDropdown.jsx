import { useEffect, useRef, useState } from "react";
import { useLanguage } from "../../i18n";

export default function CategoryDropdown({
    categories = [],
    value = "",
    onChange,
}) {
    const { t } = useLanguage();
    const rootRef = useRef(null);
    const triggerRef = useRef(null);
    const [open, setOpen] = useState(false);

    const options = [
        { value: "", label: t("pos.allCategories") },
        ...categories.map((category) => ({ value: category, label: category })),
    ];
    const selectedIndex = Math.max(
        0,
        options.findIndex((option) => option.value === value)
    );
    const selected = options[selectedIndex];

    useEffect(() => {
        function handleOutsideClick(event) {
            if (!rootRef.current?.contains(event.target)) setOpen(false);
        }

        function handleEscape(event) {
            if (event.key === "Escape") setOpen(false);
        }

        document.addEventListener("mousedown", handleOutsideClick);
        document.addEventListener("keydown", handleEscape);
        return () => {
            document.removeEventListener("mousedown", handleOutsideClick);
            document.removeEventListener("keydown", handleEscape);
        };
    }, []);

    function choose(nextValue) {
        onChange(nextValue);
        setOpen(false);
        triggerRef.current?.focus();
    }

    function handleTriggerKeyDown(event) {
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            setOpen((isOpen) => !isOpen);
            return;
        }

        if (event.key === "ArrowDown" || event.key === "ArrowUp") {
            event.preventDefault();
            if (!open) {
                setOpen(true);
                return;
            }
            const direction = event.key === "ArrowDown" ? 1 : -1;
            const nextIndex = (selectedIndex + direction + options.length) % options.length;
            choose(options[nextIndex].value);
            return;
        }

        if (event.key === "Home" || event.key === "End") {
            event.preventDefault();
            choose(options[event.key === "Home" ? 0 : options.length - 1].value);
        }
    }

    return (
        <div className="pos-category-dropdown" ref={rootRef}>
            <button
                ref={triggerRef}
                type="button"
                className="pos-category-trigger"
                role="combobox"
                aria-label={t("pos.categories")}
                aria-haspopup="listbox"
                aria-expanded={open}
                aria-controls="pos-category-options"
                onClick={() => setOpen((isOpen) => !isOpen)}
                onKeyDown={handleTriggerKeyDown}
            >
                <span>{selected.label}</span>
                <span className={`pos-dropdown-chevron${open ? " open" : ""}`} aria-hidden="true" />
            </button>

            {open && (
                <div
                    id="pos-category-options"
                    className="pos-category-options"
                    role="listbox"
                    aria-label={t("pos.categories")}
                >
                    {options.map((option) => (
                        <button
                            type="button"
                            key={option.value || "all"}
                            className={`pos-category-option${option.value === value ? " selected" : ""}`}
                            role="option"
                            aria-selected={option.value === value}
                            onClick={() => choose(option.value)}
                        >
                            <span>{option.label}</span>
                            {option.value === value && <span aria-hidden="true">✓</span>}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}
