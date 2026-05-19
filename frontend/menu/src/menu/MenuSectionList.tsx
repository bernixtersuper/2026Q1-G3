import { useEffect, useRef } from 'react';
import { MenuItemCard } from './MenuItemCard';
import { menuApi } from '@/shared/api/menuApi';
import { getSessionId } from '@/lib/utils';
import type { MenuSection } from '@/shared/types';

interface MenuSectionListProps {
  sections: MenuSection[];
  slug: string;
}

export function MenuSectionList({ sections, slug }: MenuSectionListProps) {
  const firedSections = useRef<Set<string>>(new Set());

  useEffect(() => {
    const observers: IntersectionObserver[] = [];
    const sectionElements = document.querySelectorAll('[data-section-id]');

    sectionElements.forEach((element) => {
      const sectionId = element.getAttribute('data-section-id');
      if (!sectionId) return;

      const observer = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting && !firedSections.current.has(sectionId)) {
              firedSections.current.add(sectionId);
              const sessionId = getSessionId();
              menuApi.recordEvent(slug, {
                eventType: 'SECTION_VIEW',
                sectionId,
                sessionId,
              });
            }
          });
        },
        { threshold: 0.5 }
      );

      observer.observe(element);
      observers.push(observer);
    });

    return () => {
      observers.forEach((observer) => observer.disconnect());
    };
  }, [sections, slug]);

  return (
    <div className="space-y-8">
      {sections.map((section) => (
          <section key={section.id} data-section-id={section.id}>
            <h2 className="text-xl font-semibold mb-4 text-foreground sticky top-0 bg-background py-2 z-10">
              {section.name}
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {section.items.map((item) => (
                  <MenuItemCard key={item.id} item={item} slug={slug} />
                ))}
            </div>
          </section>
        ))}
    </div>
  );
}
